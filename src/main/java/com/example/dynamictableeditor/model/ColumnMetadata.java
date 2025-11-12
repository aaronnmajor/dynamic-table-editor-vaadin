package com.example.dynamictableeditor.model;

/**
 * Represents metadata for a database table column
 */
public class ColumnMetadata {
    private String columnName;
    private String dataType;
    private boolean nullable;
    private boolean primaryKey;
    private boolean autoIncrement;
    private Integer maxLength;

    public ColumnMetadata(String columnName, String dataType, boolean nullable, 
                         boolean primaryKey, boolean autoIncrement, Integer maxLength) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.autoIncrement = autoIncrement;
        this.maxLength = maxLength;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
}
