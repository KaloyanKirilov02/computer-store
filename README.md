# Online Store Object-Relational Database Project

This project demonstrates an **object-relational database** in Oracle XE 21c with a corresponding **Java Swing UI application** using JDBC. The project implements a complete CRUD system for managing an online store, including clients, employees, products, orders, promotions, payments, deliveries, and reviews.

---

## Project Overview

- **Database**: Oracle XE 21c, using object types (`CREATE TYPE`) and object tables (`CREATE TABLE OF`).
- **Relationships**:  
  - One-to-many (client → orders, employee → orders)  
  - Many-to-many (order ↔ product, product ↔ promotion)  
  - Object references (`REF`) used for relationships
- **Sequences**: Used for auto-generating primary keys for all entities.
- **Java UI**: Swing panels for each entity with Add, Update, Delete, and Refresh functionality.
- **Reports**: Advanced JOIN queries to combine orders, products, promotions, clients, and reviews.

---

## Features

### CRUD Panels

- **Clients**: Manage client information (name, email, phone, type, etc.).
- **Employees**: Manage employee records.
- **Products**: Handles both `Computer` and `Accessory` products using inheritance.
- **Orders**: Create orders linked to clients and employees.
- **Payments & Deliveries**: Track payments and deliveries for orders.
- **Promotions**: Manage discounts and promotion periods.
- **Reviews**: Record client feedback for products.
- **Product Promotions**: Link products to applicable promotions.

### Reports

- View all orders with client, employee, payment, and delivery details.
- Orders with product details and quantities (M:N relationship).
- Products with promotions, discounts, and reviews.
- Custom SELECT-only queries for testing and reporting purposes.


---

## Validation

- Centralized validation layer implemented in `Validator`.
- All write operations validate input **before database access**.
- Includes validation for:
  - IDs and numeric values
  - Required string fields
  - Enumerated statuses (orders, payments)
  - Date values and date ranges

---

## Reports Design Note

- `ReportDAO` executes **dynamic SELECT-only queries**.
- PreparedStatement caching is intentionally **not used** for reports to avoid memory leaks.
- This behavior is intentional and documented.

---

## Technology Stack

- **Database**: Oracle XE 21c
- **Java Version**: JDK 17+
- **IDE**: IntelliJ IDEA
- **Libraries**: `ojdbc11.jar` (Oracle JDBC driver)
- **UI**: Java Swing

---

## Setup Instructions

1. **Database Setup**  
   - Use Oracle SQL Developer to create the object types, tables, and sequences.
   - Populate tables with sample data (clients, products, orders, payments, deliveries, promotions, reviews).

2. **Java Project Setup**  
   - Import the project into IntelliJ IDEA.
   - Add `ojdbc11.jar` to project libraries.
   - Configure database connection in `DBConnection.java`:

   ```java
   String url = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
   String user = "YOUR_USER";
   String password = "YOUR_PASSWORD";
   ```

3. **Run the Application**  
   - Run `MainFrame`.
   - Use the tab-based UI to test CRUD functionality.
   - Use `ReportsPanel` to execute SELECT queries and view reports.


---

## Project Highlights

- **True Object-Relational Database Design**
- **PreparedStatement & Connection Caching**
- **Centralized Validation**
- **Safe Dynamic Reporting**
- **Clean DAO / UI Separation**
