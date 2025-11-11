package com.example.dynamictableeditor.view;

import com.example.dynamictableeditor.model.ColumnMetadata;
import com.example.dynamictableeditor.service.DatabaseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.*;

/**
 * Main view for dynamic table editor
 */
@Route("")
public class MainView extends VerticalLayout {

    private final DatabaseService databaseService;
    private ComboBox<String> tableSelector;
    private Grid<Map<String, Object>> dataGrid;
    private VerticalLayout contentLayout;
    private String currentTable;
    private List<ColumnMetadata> currentColumns;

    public MainView(DatabaseService databaseService) {
        this.databaseService = databaseService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        createHeader();
        createTableSelector();
        createContentLayout();
        
        loadTables();
    }

    private void createHeader() {
        H1 title = new H1("Dynamic Table Editor");
        title.getStyle().set("margin-top", "0");
        add(title);
    }

    private void createTableSelector() {
        H3 selectorLabel = new H3("Select a Table:");
        selectorLabel.getStyle().set("margin-bottom", "10px");
        
        tableSelector = new ComboBox<>();
        tableSelector.setPlaceholder("Choose a table...");
        tableSelector.setWidth("300px");
        tableSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                loadTableData(event.getValue());
            }
        });
        
        add(selectorLabel, tableSelector);
    }

    private void createContentLayout() {
        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        add(contentLayout);
    }

    private void loadTables() {
        try {
            List<String> tables = databaseService.getAllTables();
            tableSelector.setItems(tables);
            
            if (!tables.isEmpty()) {
                tableSelector.setValue(tables.get(0));
            }
        } catch (Exception e) {
            showError("Error loading tables: " + e.getMessage());
        }
    }

    private void loadTableData(String tableName) {
        try {
            currentTable = tableName;
            currentColumns = databaseService.getColumnMetadata(tableName);
            
            contentLayout.removeAll();
            
            // Create action buttons
            HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setSpacing(true);
            
            Button addButton = new Button("Add New Record", event -> openAddDialog());
            addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button refreshButton = new Button("Refresh", event -> refreshGrid());
            
            buttonLayout.add(addButton, refreshButton);
            
            // Create grid
            dataGrid = new Grid<>();
            dataGrid.setSizeFull();
            
            // Add columns dynamically
            for (ColumnMetadata column : currentColumns) {
                dataGrid.addColumn(row -> {
                    Object value = row.get(column.getColumnName());
                    return value != null ? value.toString() : "";
                }).setHeader(column.getColumnName())
                  .setResizable(true)
                  .setSortable(true);
            }
            
            // Add action column
            dataGrid.addComponentColumn(row -> {
                HorizontalLayout actions = new HorizontalLayout();
                actions.setSpacing(true);
                
                Button editButton = new Button("Edit", event -> openEditDialog(row));
                editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
                
                Button deleteButton = new Button("Delete", event -> deleteRow(row));
                deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                
                actions.add(editButton, deleteButton);
                return actions;
            }).setHeader("Actions").setWidth("180px").setFlexGrow(0);
            
            // Load data
            refreshGrid();
            
            contentLayout.add(buttonLayout, dataGrid);
            
        } catch (Exception e) {
            showError("Error loading table data: " + e.getMessage());
        }
    }

    private void refreshGrid() {
        try {
            List<Map<String, Object>> data = databaseService.getTableData(currentTable);
            dataGrid.setItems(data);
            showSuccess("Data refreshed");
        } catch (Exception e) {
            showError("Error refreshing data: " + e.getMessage());
        }
    }

    private void openAddDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Record to " + currentTable);
        dialog.setWidth("600px");
        
        FormLayout form = createForm(new HashMap<>(), false);
        
        Button saveButton = new Button("Save", event -> {
            try {
                Map<String, Object> data = extractFormData(form);
                databaseService.validateData(currentTable, data, false);
                databaseService.insertRow(currentTable, data);
                showSuccess("Record added successfully");
                refreshGrid();
                dialog.close();
            } catch (Exception e) {
                showError("Error adding record: " + e.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", event -> dialog.close());
        
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(form);
        dialog.open();
    }

    private void openEditDialog(Map<String, Object> rowData) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Record in " + currentTable);
        dialog.setWidth("600px");
        
        FormLayout form = createForm(rowData, true);
        
        // Get primary key value
        Object primaryKeyValue = currentColumns.stream()
            .filter(ColumnMetadata::isPrimaryKey)
            .findFirst()
            .map(col -> rowData.get(col.getColumnName()))
            .orElse(null);
        
        Button saveButton = new Button("Update", event -> {
            try {
                Map<String, Object> data = extractFormData(form);
                databaseService.validateData(currentTable, data, true);
                databaseService.updateRow(currentTable, data, primaryKeyValue);
                showSuccess("Record updated successfully");
                refreshGrid();
                dialog.close();
            } catch (Exception e) {
                showError("Error updating record: " + e.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", event -> dialog.close());
        
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(form);
        dialog.open();
    }

    private FormLayout createForm(Map<String, Object> data, boolean isEdit) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1)
        );
        
        for (ColumnMetadata column : currentColumns) {
            // Skip auto-increment columns on add
            if (!isEdit && column.isAutoIncrement()) {
                continue;
            }
            
            TextField field = new TextField(column.getColumnName());
            field.setId(column.getColumnName());
            
            // Set value if editing
            if (data.containsKey(column.getColumnName())) {
                Object value = data.get(column.getColumnName());
                field.setValue(value != null ? value.toString() : "");
            }
            
            // Mark required fields
            if (!column.isNullable() && !column.isAutoIncrement()) {
                field.setRequiredIndicatorVisible(true);
            }
            
            // Disable primary key field when editing
            if (isEdit && column.isPrimaryKey()) {
                field.setReadOnly(true);
            }
            
            // Add helper text
            String helperText = buildHelperText(column);
            if (!helperText.isEmpty()) {
                field.setHelperText(helperText);
            }
            
            form.add(field);
        }
        
        return form;
    }

    private String buildHelperText(ColumnMetadata column) {
        List<String> hints = new ArrayList<>();
        
        if (column.isPrimaryKey()) {
            hints.add("Primary Key");
        }
        if (column.isAutoIncrement()) {
            hints.add("Auto-increment");
        }
        if (!column.isNullable()) {
            hints.add("Required");
        }
        
        String typeHint = null;
        String dataType = column.getDataType();
        if (dataType.contains("INT")) {
            typeHint = "Integer value";
        } else if (dataType.contains("DECIMAL") || dataType.contains("NUMERIC")) {
            typeHint = "Decimal number";
        } else if (dataType.equals("BOOLEAN") || dataType.equals("BOOL")) {
            typeHint = "true or false";
        } else if (dataType.equals("DATE")) {
            typeHint = "Format: YYYY-MM-DD";
        } else if (dataType.equals("TIMESTAMP")) {
            typeHint = "Format: YYYY-MM-DD HH:MM:SS";
        }
        
        if (typeHint != null) {
            hints.add(typeHint);
        }
        
        if (column.getMaxLength() != null && column.getMaxLength() > 0) {
            hints.add("Max length: " + column.getMaxLength());
        }
        
        return String.join(" | ", hints);
    }

    private Map<String, Object> extractFormData(FormLayout form) {
        Map<String, Object> data = new HashMap<>();
        
        form.getChildren().forEach(component -> {
            if (component instanceof TextField field) {
                String id = field.getId().orElse(null);
                if (id != null) {
                    String value = field.getValue();
                    data.put(id, value);
                }
            }
        });
        
        return data;
    }

    private void deleteRow(Map<String, Object> rowData) {
        try {
            Object primaryKeyValue = currentColumns.stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .findFirst()
                .map(col -> rowData.get(col.getColumnName()))
                .orElseThrow(() -> new RuntimeException("No primary key found"));
            
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Confirm Delete");
            confirmDialog.add("Are you sure you want to delete this record?");
            
            Button confirmButton = new Button("Delete", event -> {
                try {
                    databaseService.deleteRow(currentTable, primaryKeyValue);
                    showSuccess("Record deleted successfully");
                    refreshGrid();
                    confirmDialog.close();
                } catch (Exception e) {
                    showError("Error deleting record: " + e.getMessage());
                }
            });
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            
            Button cancelButton = new Button("Cancel", event -> confirmDialog.close());
            
            confirmDialog.getFooter().add(cancelButton, confirmButton);
            confirmDialog.open();
            
        } catch (Exception e) {
            showError("Error deleting record: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
