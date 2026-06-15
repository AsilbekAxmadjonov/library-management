# 📚 Library Management System

A full-featured REST API for managing library books, members, loans, fines, and reservations — built with **Spring Boot 3.5.x**, **PostgreSQL**, and **Liquibase**.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [Domain Model](#4-domain-model)
5. [Business Rules](#5-business-rules)
6. [Prerequisites](#6-prerequisites)
7. [Database Setup](#7-database-setup)
8. [Configuration](#8-configuration)
9. [Running the Application](#9-running-the-application)
10. [API Reference](#10-api-reference)
11. [Swagger UI](#11-swagger-ui)
12. [Running Tests](#12-running-tests)
13. [Scheduled Jobs](#13-scheduled-jobs)
14. [Error Handling](#14-error-handling)
15. [Example Requests](#15-example-requests)

---

## 1. Project Overview

The Library Management System tracks books, members, and loans. It enforces borrowing limits, calculates overdue fines automatically, supports reservation queues, and generates reports — all exposed through a REST API.

### Key Features

| Feature | Description |
|---|---|
| Book catalog | Add, search (paginated), filter by title / author / genre |
| Member management | Register members, block/activate based on fines |
| Loan lifecycle | Issue → extend → return, with all business rule validation |
| Fine calculation | Automatic daily fine accumulation for overdue loans |
| Reservation queue | Queue for unavailable books; notifies next member on return |
| Scheduled job | Runs daily at 01:00 to update fines and member statuses |
| Reports | Most-read books, members with overdue loans, fine statistics |
| Swagger UI | Interactive API documentation at `/swagger-ui.html` |

---

## 2. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.14 |
| Database | PostgreSQL | 14+ |
| ORM | Spring Data JPA (Hibernate) | 6.6.x |
| Migrations | Liquibase | 4.31.x |
| Mapping | MapStruct | 1.5.5 |
| Boilerplate reduction | Lombok | 1.18.32 |
| API Docs | Springdoc OpenAPI | 2.8.8 |
| Build | Maven | 3.9+ |
| Tests | JUnit 5 + Mockito | — |

---

## 3. Project Structure

```
src/
├── main/
│   ├── java/com/library/management/
│   │   ├── LibraryManagementSystemApplication.java   # Entry point
│   │   │
│   │   ├── config/
│   │   │   ├── LibraryProperties.java                # All business config (loan limits, fine rates, etc.)
│   │   │   └── OpenApiConfig.java                    # Swagger UI setup
│   │   │
│   │   ├── controller/                               # REST layer — thin, delegates to services
│   │   │   ├── AuthorController.java
│   │   │   ├── BookController.java
│   │   │   ├── MemberController.java
│   │   │   ├── LoanController.java
│   │   │   ├── FineController.java
│   │   │   ├── ReservationController.java
│   │   │   └── ReportController.java
│   │   │
│   │   ├── service/                                  # Business logic interfaces
│   │   │   ├── AuthorService.java
│   │   │   ├── BookService.java
│   │   │   ├── MemberService.java
│   │   │   ├── LoanService.java
│   │   │   ├── FineService.java
│   │   │   ├── ReservationService.java
│   │   │   ├── ReportService.java
│   │   │   └── impl/                                 # Implementations
│   │   │       ├── AuthorServiceImpl.java
│   │   │       ├── BookServiceImpl.java
│   │   │       ├── MemberServiceImpl.java
│   │   │       ├── LoanServiceImpl.java
│   │   │       ├── FineServiceImpl.java
│   │   │       ├── ReservationServiceImpl.java
│   │   │       └── ReportServiceImpl.java
│   │   │
│   │   ├── repository/                               # Spring Data JPA interfaces
│   │   │   ├── AuthorRepository.java
│   │   │   ├── BookRepository.java
│   │   │   ├── MemberRepository.java
│   │   │   ├── LoanRepository.java
│   │   │   ├── FineRepository.java
│   │   │   └── ReservationRepository.java
│   │   │
│   │   ├── domain/
│   │   │   ├── entity/                               # JPA entities
│   │   │   │   ├── BaseEntity.java                   # id, createdAt, updatedAt
│   │   │   │   ├── Author.java
│   │   │   │   ├── Book.java
│   │   │   │   ├── Member.java
│   │   │   │   ├── Loan.java
│   │   │   │   ├── Fine.java
│   │   │   │   └── Reservation.java
│   │   │   └── enums/
│   │   │       ├── MemberStatus.java                 # ACTIVE, BLOCKED
│   │   │       ├── MemberType.java                   # STANDARD, STUDENT, PREMIUM
│   │   │       ├── LoanStatus.java                   # ACTIVE, RETURNED, OVERDUE
│   │   │       ├── FineStatus.java                   # PENDING, PAID
│   │   │       └── ReservationStatus.java            # WAITING, NOTIFIED, CANCELLED, FULFILLED
│   │   │
│   │   ├── dto/
│   │   │   ├── request/                              # Incoming request bodies
│   │   │   │   ├── CreateAuthorRequest.java
│   │   │   │   ├── CreateBookRequest.java
│   │   │   │   ├── CreateMemberRequest.java
│   │   │   │   └── IssueLoanRequest.java
│   │   │   └── response/                             # Outgoing response bodies
│   │   │       ├── AuthorResponse.java
│   │   │       ├── BookResponse.java
│   │   │       ├── MemberResponse.java
│   │   │       ├── LoanResponse.java
│   │   │       ├── FineResponse.java
│   │   │       ├── ReservationResponse.java
│   │   │       ├── FineStatsResponse.java
│   │   │       └── PageResponse.java                 # Generic paginated wrapper
│   │   │
│   │   ├── mapper/                                   # MapStruct entity <-> DTO mappers
│   │   │   ├── AuthorMapper.java
│   │   │   ├── BookMapper.java
│   │   │   ├── MemberMapper.java
│   │   │   ├── LoanMapper.java
│   │   │   ├── FineMapper.java
│   │   │   └── ReservationMapper.java
│   │   │
│   │   └── exception/                                # Error handling
│   │       ├── BusinessException.java
│   │       ├── ErrorCode.java
│   │       ├── ErrorResponse.java
│   │       └── GlobalExceptionHandler.java
│   │
│   └── resources/
│       ├── application.yml                           # All configuration
│       └── db/changelog/
│           ├── db.changelog-master.xml               # Liquibase master file
│           └── changes/
│               ├── 001-create-authors.sql
│               ├── 002-create-members.sql
│               ├── 003-create-books.sql
│               ├── 004-create-loans.sql
│               ├── 005-create-fines.sql
│               └── 006-create-reservations.sql
│
└── test/
    └── java/com/library/management/
        └── service/
            ├── LoanServiceTest.java
            └── FineServiceTest.java
```

---

## 4. Domain Model

### Entity Relationships

```
Author (1) ──────────── (N) Book
                              │
Member (1) ──── (N) Loan ────┘
   │               │
   │               └── (1) Fine
   │
   └────── (N) Reservation ── Book
```

### Entities Explained

#### Author
Represents a book author.

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated primary key |
| firstName | String | Author's first name |
| lastName | String | Author's last name |
| bio | String | Optional biography (max 500 chars) |
| createdAt | LocalDateTime | Auto-set on insert |
| updatedAt | LocalDateTime | Auto-set on insert and update |

#### Book
A book title in the library. One book can have multiple physical copies.

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated primary key |
| title | String | Book title |
| isbn | String | Unique ISBN (optional) |
| author | Author | Many-to-one relationship |
| totalCopies | int | Total physical copies owned |
| availableCopies | int | Copies not currently on loan |
| genre | String | Genre (optional) |
| publicationYear | int | Year published |
| price | Long | Price in tiyin — used as fine cap (optional) |

#### Member
A library member who can borrow books.

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated primary key |
| firstName | String | Member's first name |
| lastName | String | Member's last name |
| email | String | Unique email address |
| phone | String | Optional phone number |
| status | MemberStatus | ACTIVE or BLOCKED |
| type | MemberType | STANDARD, STUDENT, or PREMIUM |

#### Loan
Represents one borrowing event — one member borrowing one book.

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated primary key |
| member | Member | Who borrowed the book |
| book | Book | Which book was borrowed |
| loanDate | LocalDate | Date the book was issued |
| dueDate | LocalDate | Date the book must be returned by |
| returnDate | LocalDate | Actual return date (null if not returned) |
| status | LoanStatus | ACTIVE, RETURNED, or OVERDUE |
| extensionCount | int | How many times the due date has been extended |

#### Fine
A penalty record linked to one overdue loan.

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated primary key |
| loan | Loan | The overdue loan this fine belongs to |
| amount | Long | Fine amount in tiyin |
| status | FineStatus | PENDING or PAID |
| calculatedUpTo | LocalDate | Last date the fine was calculated for (used for idempotency) |
| paidAt | LocalDateTime | Timestamp when the fine was paid |

#### Reservation
A queue entry when a member wants a book with no available copies.

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated primary key |
| member | Member | Who is waiting |
| book | Book | Which book they are waiting for |
| reservedAt | LocalDateTime | When they joined the queue |
| status | ReservationStatus | WAITING, NOTIFIED, CANCELLED, or FULFILLED |
| expiresAt | LocalDate | Deadline to pick up after being notified (3 days) |

---

## 5. Business Rules

### Issuing a Loan
A loan is rejected if any of the following are true:

| Rule | Config key | Standard | Student | Premium |
|---|---|---|---|---|
| Member is BLOCKED | — | — | — | — |
| Member already has N active loans | `library.member-types.<type>.max-books` | 5 | 3 | 10 |
| Member has unpaid fines above threshold | `library.member-types.<type>.max-unpaid-threshold` | 50 000 | 25 000 | 100 000 |
| No available copies of the book | — | — | — | — |

### Returning a Book
- Loan is closed, `returnDate` is set to today
- `availableCopies` on the book is incremented by 1
- If the return is late, a fine is automatically created
- The first person in the reservation queue (if any) is notified
### Fine Calculation
```
fine amount = billable overdue days × daily rate
billable days = overdue days − grace period days
```

Fine rates and grace periods are **per member type** — configured in `application.yml`:

| Config key | Standard | Student | Premium |
|---|---|---|---|
| `library.member-types.<type>.daily-rate` | 500 tiyin | 250 tiyin | 750 tiyin |
| `library.member-types.<type>.grace-period-days` | 0 | 2 | 1 |

- If the book has a `price` set, the fine is capped at that price
- Fine grows every day until the book is returned
- The daily scheduler recalculates all active overdue fines at 01:00 AM
### Extending a Loan
Extension is **not allowed** if:
- The loan is already returned
- The loan is already overdue
- The member has used all allowed extensions (`library.member-types.<type>.max-extensions`)
- There is a reservation queue for that book (other members are waiting)
  Default extension limits per member type:

| Type | Max extensions | Extension days |
|---|---|---|
| STANDARD | 2 | 7 |
| STUDENT | 1 | 7 |
| PREMIUM | 4 | 7 |

Each extension adds `library.loan.extension-days` days (default: 7) to the due date.

### Auto-blocking Members
The daily scheduler automatically blocks a member when their total unpaid fines exceed
`library.member-types.<type>.max-unpaid-threshold`. When they pay enough fines to drop
below the threshold, they are automatically re-activated.

### Reservation Queue
- A member can only reserve a book when `availableCopies == 0`
- Queue is FIFO (first reserved = first notified)
- When a book is returned, the first WAITING reservation is set to NOTIFIED
  with a pickup deadline of `library.reservation.notification-expiry-days` days (default: 3)
- A member can cancel their own reservation at any time if it is still WAITING or NOTIFIED
---

## 6. Prerequisites

Make sure the following are installed before running the project:

| Tool | Minimum Version | Check command |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| PostgreSQL | 14+ | `psql --version` |

> ⚠️ The project **will not compile** on JDK 25 due to MapStruct annotation processor incompatibility. Use JDK 21 exactly.

---

## 7. Database Setup

### Step 1 — Create the database

Connect to PostgreSQL and run:

```sql
CREATE DATABASE library_db;
```

If you want a dedicated user:

```sql
CREATE USER library_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE library_db TO library_user;
```

### Step 2 — Tables are created automatically

**You do not need to run any SQL manually.** When the application starts, Liquibase automatically runs all migration scripts in order and creates all tables:

| Migration file | What it creates |
|---|---|
| `001-create-authors.sql` | `authors` table + sequence |
| `002-create-members.sql` | `members` table + sequence |
| `003-create-books.sql` | `books` table + sequence |
| `004-create-loans.sql` | `loans` table + sequence + indexes |
| `005-create-fines.sql` | `fines` table + sequence |
| `006-create-reservations.sql` | `reservations` table + sequence + indexes |

Liquibase also creates two tracking tables automatically:
- `databasechangelog` — records which migrations have been run
- `databasechangeloglock` — prevents concurrent migrations

### Resetting the database (development only)

If you need to start fresh:

```sql
DROP TABLE IF EXISTS reservations, fines, loans, books, members, authors CASCADE;
DROP SEQUENCE IF EXISTS authors_seq, members_seq, books_seq, loans_seq, fines_seq, reservations_seq;
DROP TABLE IF EXISTS databasechangelog, databasechangeloglock CASCADE;
```

Then restart the application — Liquibase will recreate everything.

---

## 8. Configuration

All configuration is in `src/main/resources/application.yml`.

### Database connection

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/library_db
    username: postgres
    password: postgres
```

### Business rules (all configurable — nothing hardcoded)

```yaml
library:
  loan:
    default-loan-days: 14       # Due date = today + this many days
    extension-days: 7           # Days added to due date per extension
 
  member-types:
    standard:
      daily-rate: 500           # Tiyin charged per overdue day
      max-books: 5              # Max active loans at once
      max-extensions: 2         # Max loan extensions
      grace-period-days: 0      # Grace days before fine kicks in
      max-unpaid-threshold: 50000
 
    student:
      daily-rate: 250
      max-books: 3
      max-extensions: 1
      grace-period-days: 2
      max-unpaid-threshold: 25000
 
    premium:
      daily-rate: 750
      max-books: 10
      max-extensions: 4
      grace-period-days: 1
      max-unpaid-threshold: 100000
 
  reservation:
    notification-expiry-days: 3  # Days a notified member has to pick up the book
 
  scheduler:
    fine-update-cron: "0 0 1 * * *"  # Daily at 01:00 AM
```

---

## 9. Running the Application

### Clone the project

```bash
git clone <repository-url>
cd library-management
```

### Build

```bash
mvn clean install -DskipTests
```

### Run

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/library-management-system-0.0.1-SNAPSHOT.jar
```

### Verify startup

When the application starts successfully you will see:

```
Started LibraryManagementSystemApplication in X.XXX seconds
```

And Liquibase will log each migration:

```
Running Changeset: db/changelog/changes/001-create-authors.sql::001-create-authors-seq
Running Changeset: db/changelog/changes/001-create-authors.sql::001-create-authors-table
...
```

### Verify the API is running

```bash
curl http://localhost:8080/api/v1/authors
```

Expected response: `[]` (empty array on a fresh database)

---

## 10. API Reference

Base URL: `http://localhost:8080`

### Authors

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| POST | `/api/v1/authors` | Create a new author | `CreateAuthorRequest` |
| GET | `/api/v1/authors` | Get all authors | — |
| GET | `/api/v1/authors/{id}` | Get author by ID | — |
| PUT | `/api/v1/authors/{id}` | Update author | `CreateAuthorRequest` |
| DELETE | `/api/v1/authors/{id}` | Delete author | — |

### Books

| Method | Endpoint | Description | Notes |
|---|---|---|---|
| POST | `/api/v1/books` | Add a new book | — |
| GET | `/api/v1/books` | Search books (paginated) | Query params: `title`, `authorName`, `genre`, `page`, `size`, `sortBy` |
| GET | `/api/v1/books/{id}` | Get book by ID | — |
| PUT | `/api/v1/books/{id}` | Update book | — |
| DELETE | `/api/v1/books/{id}` | Delete book | — |

### Members

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/members` | Register a new member |
| GET | `/api/v1/members` | Get all members |
| GET | `/api/v1/members/{id}` | Get member by ID |
| PUT | `/api/v1/members/{id}` | Update member info |
| PATCH | `/api/v1/members/{id}/block` | Manually block a member |
| PATCH | `/api/v1/members/{id}/activate` | Manually activate a member |
| DELETE | `/api/v1/members/{id}` | Delete member |

### Loans

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/loans` | Issue a book to a member |
| GET | `/api/v1/loans/{id}` | Get loan by ID |
| GET | `/api/v1/loans/member/{memberId}` | Get all loans for a member |
| PATCH | `/api/v1/loans/{id}/return` | Return a book |
| PATCH | `/api/v1/loans/{id}/extend` | Extend the due date |

### Fines

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/fines/{id}` | Get fine by ID |
| GET | `/api/v1/fines/member/{memberId}` | Get all fines for a member |
| PATCH | `/api/v1/fines/{id}/pay` | Pay a fine |

### Reservations

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/reservations?memberId=X&bookId=Y` | Reserve a book |
| GET | `/api/v1/reservations/member/{memberId}` | Get member's reservations |
| PATCH | `/api/v1/reservations/{id}/cancel?memberId=X` | Cancel a reservation |

### Reports

| Method | Endpoint | Description | Notes |
|---|---|---|---|
| GET | `/api/v1/reports/books/most-read` | Most borrowed books | Query param: `limit` (default 10) |
| GET | `/api/v1/reports/members/overdue` | Members with overdue loans | — |
| GET | `/api/v1/reports/fines/stats` | Fine statistics | Returns total count, total amount, paid vs unpaid |

---

## 11. Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI JSON spec:

```
http://localhost:8080/api-docs
```

Swagger UI lets you:
- See all endpoints grouped by tag
- Read request/response schemas
- Send real requests directly from the browser

---

## 12. Running Tests

### Run all tests

```bash
mvn test
```

### Run a specific test class

```bash
mvn test -Dtest=LoanServiceTest
mvn test -Dtest=FineServiceTest
```

### What is tested

#### `LoanServiceTest`
Pure unit tests using Mockito — no Spring context, no database.

| Test | What it verifies |
|---|---|
| `issueLoan_happyPath_createsLoan` | Loan is created, available copies decremented |
| `issueLoan_blockedMember_throwsException` | BLOCKED member cannot borrow |
| `issueLoan_noCopies_throwsException` | Book with 0 copies cannot be borrowed |
| `issueLoan_loanLimitReached_throwsException` | Member at max loans cannot borrow more |
| `returnBook_overdue_createsFine` | Returning late creates a fine with correct amount |
| `returnBook_onTime_noFine` | Returning on time creates no fine |
| `extendLoan_overdue_throwsException` | Overdue loan cannot be extended |
| `extendLoan_maxExtensionsReached_throwsException` | Cannot extend beyond limit |

#### `FineServiceTest`
Pure unit tests for fine calculation and payment.

| Test | What it verifies |
|---|---|
| `updateOverdueFines_idempotent_doesNotDuplicate` | Running the job twice on the same day updates nothing |
| `payFine_marksAsPaid` | Fine status becomes PAID |
| `payFine_alreadyPaid_throwsException` | Cannot pay a fine that is already paid |
| `payFine_unblocksMemberWhenThresholdMet` | Member is re-activated when unpaid fines drop below threshold |

---

## 13. Scheduled Jobs

### Daily Fine Update (`FineServiceImpl.runDailyFineUpdate`)

Runs every day at **01:00 AM** (configurable via `library.scheduler.fine-update-cron`).

**What it does:**
1. Finds all ACTIVE loans where `dueDate < today`
2. For each overdue loan, calculates: `overdueDays × dailyRate`
3. Creates or updates the Fine record for that loan
4. Updates the loan status to OVERDUE
5. Checks if the member's total unpaid fines exceed the threshold → auto-blocks if so

**Idempotency:**
Each `Fine` has a `calculatedUpTo` field. The job checks this field first — if it already equals today's date, that fine is skipped entirely. This means running the job multiple times in one day is completely safe and produces no duplicate data.

---

## 14. Error Handling

All errors return a consistent JSON format:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "fieldErrors": null,
  "timestamp": "2026-06-04T10:00:00"
}
```

For validation errors, `fieldErrors` contains a map of field → error message:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": {
    "email": "must be a well-formed email address",
    "firstName": "must not be blank"
  },
  "timestamp": "2026-06-04T10:00:00"
}
```

### HTTP Status Codes

| Status | When |
|---|---|
| `200 OK` | Successful GET, PATCH, PUT |
| `201 Created` | Successful POST |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation failure |
| `403 Forbidden` | Member is blocked |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate resource or invalid state (e.g. already returned) |
| `422 Unprocessable Entity` | Business rule violation (e.g. loan limit exceeded) |
| `500 Internal Server Error` | Unexpected server error |

### Error Codes

| Code | Meaning |
|---|---|
| `RESOURCE_NOT_FOUND` | Entity with given ID does not exist |
| `MEMBER_BLOCKED` | Member status is BLOCKED |
| `NO_COPIES_AVAILABLE` | All copies of a book are on loan |
| `LOAN_LIMIT_EXCEEDED` | Member has reached the max concurrent loan limit |
| `FINE_LIMIT_EXCEEDED` | Member's unpaid fines exceed the threshold |
| `LOAN_ALREADY_RETURNED` | Trying to return a loan that is already returned |
| `EXTENSION_NOT_ALLOWED` | Loan extension rejected (overdue / at limit / reservation queue exists) |
| `ALREADY_RESERVED` | Member is already in the queue for this book |
| `VALIDATION_ERROR` | Request body failed `@Valid` validation |
| `INTERNAL_ERROR` | Unexpected server-side error |

---

## 15. Example Requests

### Create an author

```bash
curl -X POST http://localhost:8080/api/v1/authors \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Robert",
    "lastName": "Martin",
    "bio": "Author of Clean Code and Clean Architecture"
  }'
```

Response `201 Created`:
```json
{
  "id": 1,
  "firstName": "Robert",
  "lastName": "Martin",
  "bio": "Author of Clean Code and Clean Architecture",
  "createdAt": "2026-06-04T10:00:00"
}
```

---

### Add a book

```bash
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "isbn": "978-0132350884",
    "authorId": 1,
    "totalCopies": 3,
    "genre": "Programming",
    "publicationYear": 2008,
    "price": 150000
  }'
```

---

### Register a member

```bash
curl -X POST http://localhost:8080/api/v1/members \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Asilbek",
    "lastName": "Aliyev",
    "email": "asilbek@example.com",
    "phone": "+998901234567",
    "type": "STANDARD"
  }'
```

---

### Issue a loan

```bash
curl -X POST http://localhost:8080/api/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": 1,
    "bookId": 1
  }'
```

Response `201 Created`:
```json
{
  "id": 1,
  "memberId": 1,
  "memberFullName": "Asilbek Aliyev",
  "bookId": 1,
  "bookTitle": "Clean Code",
  "loanDate": "2026-06-04",
  "dueDate": "2026-06-18",
  "returnDate": null,
  "status": "ACTIVE",
  "extensionCount": 0
}
```

---

### Return a book

```bash
curl -X PATCH http://localhost:8080/api/v1/loans/1/return
```

---

### Extend a loan

```bash
curl -X PATCH http://localhost:8080/api/v1/loans/1/extend
```

---

### Pay a fine

```bash
curl -X PATCH http://localhost:8080/api/v1/fines/1/pay
```

---

### Search books with pagination

```bash
curl "http://localhost:8080/api/v1/books?title=clean&page=0&size=5&sortBy=title"
```

---

### Reserve a book

```bash
curl -X POST "http://localhost:8080/api/v1/reservations?memberId=1&bookId=1"
```

---

### Get fine statistics

```bash
curl http://localhost:8080/api/v1/reports/fines/stats
```

Response:
```json
{
  "totalFines": 12,
  "totalAmount": 85000,
  "paidAmount": 35000,
  "unpaidAmount": 50000
}
```

---

### Get most read books (top 5)

```bash
curl "http://localhost:8080/api/v1/reports/books/most-read?limit=5"
```

---

## Common Issues

| Problem | Cause | Fix |
|---|---|---|
| `created_at null` error on insert | `@EnableJpaAuditing` missing | Add it to main class |
| Two `LibraryProperties` beans | Both `@Component` and `@EnableConfigurationProperties` used | Remove `@Component` from `LibraryProperties` |
| Liquibase `relation already exists` | Tables exist but not tracked | Drop all tables + `databasechangelog` and restart |
| MapStruct `Unknown property` error | Field referenced in `@Mapping` does not exist in entity | Add the field or remove the `@Mapping` line |
| Springdoc `NoSuchMethodError` | Version mismatch with Spring Boot 3.5.x | Use `springdoc-openapi 2.8.8` |
| Compile error `javac 25` | IntelliJ using wrong JDK | Set Project SDK to Java 21 in Project Structure |