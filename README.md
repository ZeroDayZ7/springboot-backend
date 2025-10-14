# Spring Boot Test Project

A simple project for learning Spring Boot and practicing Java development.

## 🔹 Tech stack
- Java 25
- Spring Boot 3.5.6
- Spring Data JPA / Hibernate
- H2 (dev) + MySQL (prod)
- Spring Security (basic auth)
- Lombok
- Gradle

## 🔹 Project structure
```markdown
├── main
│   ├── java
│   │   └── com
│   │       └── app
│   │           └── backend
│   │               ├── BackendApplication.java
│   │               ├── config
│   │               │   ├── CorsProperties.java
│   │               │   └── SecurityConfig.java
│   │               ├── controller
│   │               │   ├── HelloController.java
│   │               │   ├── TestController.java
│   │               │   ├── UserController.java
│   │               │   └── health
│   │               │       └── HealthController.java
│   │               ├── dto
│   │               │   └── .gitkeep
│   │               ├── exception
│   │               │   ├── GlobalExceptionHandler.java
│   │               │   └── ResourceNotFoundException.java
│   │               ├── filter
│   │               │   ├── ErrorLoggingFilter.java
│   │               │   ├── GlobalRateLimiterFilter.java
│   │               │   ├── RequestLoggingFilter.java
│   │               │   ├── RequestSanitizationFilter.java
│   │               │   └── SecurityHeadersFilter.java
│   │               ├── model
│   │               │   └── User.java
│   │               ├── repository
│   │               │   └── UserRepository.java
│   │               ├── scripts
│   │               │   └── run.sh
│   │               └── service
│   │                   └── UserService.java
│   └── resources
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── application.yml
│       ├── logback-spring.xml
│       ├── static
└── test
    └── java
        └── com
            └── app
                └── backend
                    └── BackendApplicationTests.java
```

## 🔹 Getting Started

1. **Clone the repo**
```bash
git clone https://github.com/ZeroDayZ7/springboot-backend.git
cd springboot-backend
```

2. **Run the application**
```bash
./gradlew bootRun
```

3. **Access H2 Console (dev profile)**
```markdown
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./db/devdb
User: SA
Password: (leave empty)
```

4. **Test API**

- Get all users: GET http://localhost:8080/api/users
- Get user by id: GET http://localhost:8080/api/users/{id}

## 🔹 Notes

* H2 database is **for development only**
* Production uses **MySQL** (configure in `application-prod.yml`)

## 🔹 Bonus

* Project structure generated using my custom CLI tool written in Go: [`cmdr tree -c`](https://github.com/ZeroDayZ7/cmdr)

