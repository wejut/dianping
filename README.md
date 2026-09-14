A high-concurrency local life and merchant review backend platform inspired by Dianping, built with **Spring Boot 2.7, Redis, and MySQL**. This service focuses on high-concurrency cache 
architecture, solving cache consistency challenges, and optimizing read/write throughput for high-frequency merchant and shop queries.

Tech Stack

Tech Stack
Core Framework: Spring Boot, Spring MVC
Persistence Layer: MyBatis-Plus
Database & Cache: MySQL 8.0, Redis (Spring Data Redis, Redisson)
Utilities & Tools: Hutool, Lombok, Maven

Project Structure

text
heima-dianping
  src/main/java/com/hmdp
    controller    # RESTful APIs for Shops, Types, and User Interactions
    service       # Core Business Logic & Redis Cache Management
    mapper        # Database Access Layer
    dto / entity  # Data Transfer Objects & Database Entities
    utils         # Redis Cache Tools, Client Wrappers, and Interceptors
   src/main/resources
