# Yaya Restaurant Management System

## Project Overview

Yaya Restaurant Management System is a comprehensive restaurant industry solution built with the **Spring Boot** framework, implementing a front-end and back-end separation architecture. The system primarily serves two user groups: **C-end users** (customers) for online ordering, payment, and order management; and **B-end management** (merchants) for dish management, order processing, and business data analysis.

## Core Features

### User-side Features
- **User Login**: Supports WeChat authorization login to retrieve user information.
- **Dish Browsing**: View dish lists by category, with options to check detailed dish information and select flavor preferences.
- **Shopping Cart**: Flexibly add or remove items from the cart.
- **Order System**: Supports online ordering, WeChat Pay integration, order status tracking, historical order queries, and "Order Again" functionality.
- **Address Management**: Manage delivery addresses, including setting a default address.

### Management-side Features
- **Staff Management**: Create, read, update, delete staff accounts; enable/disable accounts and manage passwords.
- **Category Management**: Maintain categories for Dishes and Set Meals.
- **Dish/Set Meal Management**: CRUD operations for dish information, enable/disable sales, configure flavor options, and manage associations between set meals and dishes.
- **Order Management**: Real-time receipt of new orders with full lifecycle operations including accept, reject, cancel, deliver, and complete.
- **Table Management**: Support for adding tables, updating their status (available/occupied), and reordering.
- **Data Analytics**: Provides statistics on turnover, order volume, user growth, and best-selling items, with Excel report export functionality.

## Technology Stack

| Technology | Purpose |
| :--- | :--- |
| **Spring Boot** | Core application framework |
| **MyBatis** | Data persistence layer framework |
| **MySQL** | Relational database |
| **Redis** | Caching, session storage, API rate limiting |
| **JWT** | User authentication and authorization |
| **WebSocket** | Real-time order push notifications |
| **WeChat Pay** | Integrated Native/JSAPI payment capabilities |
| **Spring Task** | Scheduled tasks (handling expired orders) |
| **AOP** | Automatic auditing field filling (creation/update timestamps) |

## Project Structure

The project adopts a Maven multi-module design, primarily divided into the following three components:

```
yaya
├── common          # Common base module
│   ├── constant    # System constants and message definitions
│   ├── context     # Current user context (based on ThreadLocal)
│   ├── exception   # Custom business exception classes
│   ├── properties  # Configuration mapping classes (JWT, WeChat settings)
│   ├── result      # Unified API response encapsulation
│   └── utils       # General utility classes (JWT, HttpClient, WeChat Pay)
│
├── pojo            # Entity and data transfer module
│   ├── dto         # Data Transfer Objects (for requests/responses)
│   ├── entity      # Database entity classes
│   └── vo          # View Objects (for frontend display)
│
└── server          # Main business service module
    ├── config      # WebMvc, Redis, WebSocket configurations
    ├── controller  # Controller layer (separated Admin and User endpoints)
    ├── interceptor # JWT interceptors (admin and user authentication)
    ├── mapper      # MyBatis Mapper interfaces and XML files
    ├── service     # Business logic layer (Service interfaces and implementations)
    ├── task        # Scheduled tasks (automatic cancellation of expired orders)
    └── websocket   # WebSocket server (real-time communication)
```

## Quick Start

### Environment Preparation
- JDK 1.8+
- Maven 3.x+
- MySQL 8.0+
- Redis

### Configuration Instructions
Configure database connections, Redis connections, and third-party service keys in `server/src/main/resources/application.yml` or the corresponding environment-specific configuration file:
- **Database Configuration**: `spring.datasource` related settings.
- **Redis Configuration**: `spring.redis` related settings.
- **JWT Configuration**: Key and expiration time under `login-reg.jwt`.
- **WeChat Pay**: AppID, merchant ID, and key under `hanye.wechat`.

### Execution Steps
1. **Initialize Database**: Create the database and execute the provided SQL script to initialize table structures.
2. **Start Redis**: Ensure the Redis service is running.
3. **Build and Run**:
   ```bash
   # In the project root directory
   mvn clean install -DskipTests
   
   # Start the server
   cd server
   mvn spring-boot:run
   ```

## License

This project is licensed under an open-source license. For detailed licensing information, please refer to the LICENSE file in the project root directory.