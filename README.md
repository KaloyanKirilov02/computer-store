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

- View all orders along with client, employee, payment, and delivery details.
- Orders with product details and quantities (M:N relationship).
- Products with promotions and reviews.
- Advanced reports combining orders, products, promotions, and client data.

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
   - Run individual UI panels for testing CRUD operations.
   - Use `ReportsPanel` to execute JOIN queries and view consolidated reports.

---

## Project Highlights

- **Object-Oriented Database**: Uses Oracle object types with inheritance for products (computers/accessories).
- **M:N Relationships**: Implemented via intermediate object tables (`order_products`, `product_promotions`).
- **Ref Integrity**: Uses `REF` pointers between tables for safe navigation.
- **Seamless UI Integration**: Java Swing panels provide a user-friendly interface for all CRUD operations.
- **Advanced Reporting**: JOIN queries allow comprehensive reporting on orders, products, promotions, and reviews.

---

## Future Improvements

- Add validation and error handling in UI forms.
- Include search and filtering functionality in tables.
- Integrate modern UI frameworks (JavaFX or web-based frontend).
- Implement transaction management for multi-step operations.

---

## References

- [Oracle Object-Relational Features](https://docs.oracle.com/en/database/oracle/oracle-database/21/adobj/index.html)  
- [JDBC Developer’s Guide](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/index.html)  
- [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)

