# Metrics Service Application

## Overview

The **Metrics Service Application** provides a RESTful API to manage and retrieve speed metrics for multiple lines. It processes speed measurements, calculates metrics such as weighted averages, maximum, and minimum speeds, and ensures data accuracy by managing a time window of 60 minutes. 

Key Features:
- Add speed measurements with timestamp validation.
- Retrieve metrics for a specific line or all lines.
- Periodically clean outdated entries to maintain performance.

---

## Table of Contents

1. [Technologies Used](#technologies-used)
2. [Setup Instructions](#setup-instructions)
3. [Endpoints](#endpoints)
4. [Scheduled Tasks](#scheduled-tasks)
5. [Testing](#testing)
6. [Future Improvements](#future-improvements)

---

## Technologies Used

- **Java 11**
- **Spring Boot**
- **Mockito** (for testing)
- **JUnit 5** (for unit testing)
- **Log4j** (for logging)
- **ConcurrentHashMap** and `TreeMap` (for data storage and management)

---

## Setup Instructions

### Prerequisites

- JDK 11 installed
- Maven installed

### Build and Run

1. Clone the repository:
	```bash
   		git clone https://github.com/your-repo/metrics-service.git
   		cd challenge
	```
2. Build the application:
	```bash
	mvn clean install
	```
3. Run the application:
	```bash
	mvn spring-boot:run
	```
	
4. The API will be available at: <http://localhost:8080/api>

## Endpoints
### Add Speed Measurement ###
	POST /api/linespeed
Adds a speed measurement for a specific line. Validates the timestamp and line ID.
* Request Body:

		{
		  "lineId": 10,
		  "timestamp": 1696600000000,
		  "speed": 120.5
		}

* Responses:
	* 201 CREATED: Measurement added successfully.
	* 204 NO CONTENT: Measurement timestamp is older than the 60-minute window.
	* 404 NOT FOUND: The lineId does not exist.
    
### Get Metrics for a Specific Line ###

GET /api/metrics/{lineid}

Retrieves metrics (weighted average, max, and min speeds) for a specific line.
* Path Parameters:
	* lineid: The ID of the line (e.g., 10).
* Responses:
  * 200 OK: Metrics retrieved successfully
  
		{
		  "avg": 120.0,
		  "max": 150.0,
		  "min": 90.0
		}

* 404 NOT FOUND: No data available for the specified lineId.

### Get Metrics for All Lines ###

GET /api/metrics

Retrieves aggregated metrics for all lines with available data.

* Responses:
	* 200 OK: Aggregated metrics retrieved successfully.
	```javascript
 	[
	  {
	    "lineId": 10,
	    "avg": 120.0,
	    "max": 150.0,
	    "min": 90.0
	  },
	  {
	    "lineId": 20,
	    "avg": 130.0,
	    "max": 160.0,
	    "min": 100.0
	  }
	]
	```

### Testing

The project includes comprehensive unit tests using Mockito and JUnit 5.
**Running Tests**

To run the tests, execute:

	mvn test

**Test Coverage**

1. Controller Tests:
	* Validate request handling for all endpoints.
	* Test edge cases like invalid lineId and outdated timestamps.

2. Service Tests:
	* Test metrics recalculation logic.
	* Verify scheduled task behavior.
	* Mock dependencies to isolate business logic.
