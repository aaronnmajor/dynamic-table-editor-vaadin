-- Create sample tables with different column types

CREATE TABLE IF NOT EXISTS EMPLOYEES (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    FIRST_NAME VARCHAR(100) NOT NULL,
    LAST_NAME VARCHAR(100) NOT NULL,
    EMAIL VARCHAR(255) NOT NULL,
    DEPARTMENT VARCHAR(100),
    SALARY DECIMAL(10,2),
    HIRE_DATE DATE
);

CREATE TABLE IF NOT EXISTS PRODUCTS (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    PRODUCT_NAME VARCHAR(255) NOT NULL,
    DESCRIPTION VARCHAR(500),
    PRICE DECIMAL(10,2) NOT NULL,
    QUANTITY INT NOT NULL,
    ACTIVE BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS CUSTOMERS (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    CUSTOMER_NAME VARCHAR(255) NOT NULL,
    CONTACT_EMAIL VARCHAR(255),
    PHONE VARCHAR(50),
    ADDRESS VARCHAR(500),
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO EMPLOYEES (FIRST_NAME, LAST_NAME, EMAIL, DEPARTMENT, SALARY, HIRE_DATE) VALUES
    ('John', 'Doe', 'john.doe@example.com', 'Engineering', 75000.00, '2020-01-15'),
    ('Jane', 'Smith', 'jane.smith@example.com', 'Marketing', 65000.00, '2021-03-20'),
    ('Bob', 'Johnson', 'bob.johnson@example.com', 'Sales', 70000.00, '2019-11-10');

INSERT INTO PRODUCTS (PRODUCT_NAME, DESCRIPTION, PRICE, QUANTITY, ACTIVE) VALUES
    ('Laptop', 'High-performance laptop', 1299.99, 50, TRUE),
    ('Mouse', 'Wireless mouse', 29.99, 200, TRUE),
    ('Keyboard', 'Mechanical keyboard', 89.99, 100, TRUE);

INSERT INTO CUSTOMERS (CUSTOMER_NAME, CONTACT_EMAIL, PHONE, ADDRESS) VALUES
    ('Acme Corp', 'contact@acme.com', '555-0100', '123 Business St'),
    ('Tech Solutions', 'info@techsol.com', '555-0200', '456 Innovation Ave'),
    ('Global Enterprises', 'hello@global.com', '555-0300', '789 Commerce Blvd');
