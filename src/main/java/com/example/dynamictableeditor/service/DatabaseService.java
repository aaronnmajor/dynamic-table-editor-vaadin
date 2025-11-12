package com.example.dynamictableeditor.service;

import com.example.dynamictableeditor.model.ColumnMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Service for interacting with database metadata and performing CRUD operations
 */
@Service
public class DatabaseService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get list of all tables from the database
     */
    public List<String> getAllTables() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // Filter out system tables
                if (!tableName.startsWith("INFORMATION_SCHEMA") && 
                    !tableName.startsWith("SYSTEM_")) {
                    tables.add(tableName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching table list", e);
        }
        return tables;
    }

    /**
     * Get column metadata for a specific table
     */
    public List<ColumnMetadata> getColumnMetadata(String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Get primary keys
            Set<String> primaryKeys = new HashSet<>();
            ResultSet pkRs = metaData.getPrimaryKeys(null, null, tableName);
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            
            // Get columns
            ResultSet rs = metaData.getColumns(null, null, tableName, "%");
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                boolean isPrimaryKey = primaryKeys.contains(columnName);
                String autoInc = rs.getString("IS_AUTOINCREMENT");
                boolean autoIncrement = "YES".equalsIgnoreCase(autoInc);
                Integer maxLength = rs.getInt("COLUMN_SIZE");
                
                columns.add(new ColumnMetadata(columnName, dataType, nullable, 
                                              isPrimaryKey, autoIncrement, maxLength));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching column metadata for table: " + tableName, e);
        }
        
        return columns;
    }

    /**
     * Get all rows from a table
     */
    public List<Map<String, Object>> getTableData(String tableName) {
        String sql = "SELECT * FROM " + sanitizeTableName(tableName);
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Insert a new row into a table
     */
    public void insertRow(String tableName, Map<String, Object> data) {
        List<ColumnMetadata> columns = getColumnMetadata(tableName);
        
        StringBuilder sql = new StringBuilder("INSERT INTO " + sanitizeTableName(tableName) + " (");
        StringBuilder values = new StringBuilder(" VALUES (");
        List<Object> params = new ArrayList<>();
        
        boolean first = true;
        for (ColumnMetadata column : columns) {
            // Skip auto-increment columns
            if (column.isAutoIncrement()) {
                continue;
            }
            
            if (data.containsKey(column.getColumnName())) {
                if (!first) {
                    sql.append(", ");
                    values.append(", ");
                }
                sql.append(column.getColumnName());
                values.append("?");
                params.add(convertValue(data.get(column.getColumnName()), column.getDataType()));
                first = false;
            }
        }
        
        sql.append(")").append(values).append(")");
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * Update an existing row in a table
     */
    public void updateRow(String tableName, Map<String, Object> data, Object primaryKeyValue) {
        List<ColumnMetadata> columns = getColumnMetadata(tableName);
        String primaryKeyColumn = columns.stream()
            .filter(ColumnMetadata::isPrimaryKey)
            .findFirst()
            .map(ColumnMetadata::getColumnName)
            .orElseThrow(() -> new RuntimeException("No primary key found for table: " + tableName));
        
        StringBuilder sql = new StringBuilder("UPDATE " + sanitizeTableName(tableName) + " SET ");
        List<Object> params = new ArrayList<>();
        
        boolean first = true;
        for (ColumnMetadata column : columns) {
            // Skip primary key and auto-increment columns
            if (column.isPrimaryKey() || column.isAutoIncrement()) {
                continue;
            }
            
            if (data.containsKey(column.getColumnName())) {
                if (!first) {
                    sql.append(", ");
                }
                sql.append(column.getColumnName()).append(" = ?");
                params.add(convertValue(data.get(column.getColumnName()), column.getDataType()));
                first = false;
            }
        }
        
        sql.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
        params.add(primaryKeyValue);
        
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * Delete a row from a table
     */
    public void deleteRow(String tableName, Object primaryKeyValue) {
        List<ColumnMetadata> columns = getColumnMetadata(tableName);
        String primaryKeyColumn = columns.stream()
            .filter(ColumnMetadata::isPrimaryKey)
            .findFirst()
            .map(ColumnMetadata::getColumnName)
            .orElseThrow(() -> new RuntimeException("No primary key found for table: " + tableName));
        
        String sql = "DELETE FROM " + sanitizeTableName(tableName) + 
                    " WHERE " + primaryKeyColumn + " = ?";
        jdbcTemplate.update(sql, primaryKeyValue);
    }

    /**
     * Validate data before insert/update
     */
    public void validateData(String tableName, Map<String, Object> data, boolean isUpdate) {
        List<ColumnMetadata> columns = getColumnMetadata(tableName);
        
        for (ColumnMetadata column : columns) {
            // Skip validation for auto-increment columns on insert
            if (!isUpdate && column.isAutoIncrement()) {
                continue;
            }
            
            Object value = data.get(column.getColumnName());
            
            // Check for null values
            if (!column.isNullable() && !column.isAutoIncrement() && 
                (value == null || value.toString().trim().isEmpty())) {
                throw new IllegalArgumentException(
                    "Column '" + column.getColumnName() + "' cannot be null or empty");
            }
            
            // Type validation
            if (value != null && !value.toString().trim().isEmpty()) {
                validateType(column.getColumnName(), value, column.getDataType());
            }
        }
    }

    /**
     * Validate value type
     */
    private void validateType(String columnName, Object value, String dataType) {
        String strValue = value.toString().trim();
        
        try {
            if (dataType.contains("INT") || dataType.equals("INTEGER")) {
                Integer.parseInt(strValue);
            } else if (dataType.contains("DECIMAL") || dataType.contains("NUMERIC")) {
                Double.parseDouble(strValue);
            } else if (dataType.equals("BOOLEAN") || dataType.equals("BOOL")) {
                if (!strValue.equalsIgnoreCase("true") && !strValue.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException(
                        "Column '" + columnName + "' must be true or false");
                }
            } else if (dataType.equals("DATE")) {
                // Basic date validation (format will be handled by conversion)
                if (strValue.split("-").length != 3) {
                    throw new IllegalArgumentException(
                        "Column '" + columnName + "' must be a valid date (YYYY-MM-DD)");
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Column '" + columnName + "' must be a valid " + dataType);
        }
    }

    /**
     * Convert string value to appropriate type
     */
    private Object convertValue(Object value, String dataType) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        
        String strValue = value.toString().trim();
        
        try {
            if (dataType.contains("INT") || dataType.equals("INTEGER")) {
                return Integer.parseInt(strValue);
            } else if (dataType.contains("DECIMAL") || dataType.contains("NUMERIC")) {
                return Double.parseDouble(strValue);
            } else if (dataType.equals("BOOLEAN") || dataType.equals("BOOL")) {
                return Boolean.parseBoolean(strValue);
            } else if (dataType.equals("DATE")) {
                return java.sql.Date.valueOf(strValue);
            } else if (dataType.equals("TIMESTAMP")) {
                return java.sql.Timestamp.valueOf(strValue);
            }
        } catch (Exception e) {
            // Return as string if conversion fails
        }
        
        return strValue;
    }

    /**
     * Sanitize table name to prevent SQL injection
     */
    private String sanitizeTableName(String tableName) {
        // Only allow alphanumeric characters and underscores
        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        return tableName;
    }
}
