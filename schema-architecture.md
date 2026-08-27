# Smart Clinic Management System Architecture

## Section 1: Architecture summary

The Smart Clinic Management System uses a three-tier architecture. Its presentation tier supports server-rendered admin and doctor pages built with Thymeleaf and Spring MVC, as well as clients that communicate through REST APIs. Spring Boot forms the application tier, where controllers receive requests and delegate business operations to a shared service layer instead of accessing data sources directly.

The data tier combines MySQL and MongoDB. Spring Data JPA repositories manage structured relational data in MySQL, while MongoDB repositories manage flexible document models. This separation keeps presentation, business logic, and persistence responsibilities clear while allowing both database technologies to work through one consistent service layer.

## Section 2: Numbered flow of data and control

1. A user action begins in a Thymeleaf page or an external REST client. Thymeleaf forms send MVC requests for dashboard workflows, while API consumers send HTTP requests with parameters or JSON payloads.
2. A Spring MVC controller handles the Thymeleaf request. Spring model binding maps submitted form fields to Java model objects, and validation results can be returned to the same view when input is invalid.
3. A REST controller handles API requests, converts JSON request bodies into Java objects, validates the input, and prepares the appropriate HTTP response contract.
4. Both MVC and REST controllers delegate business rules and workflow decisions to the service layer, keeping controller code focused on request and response handling.
5. The service layer calls the required repository interfaces. Spring Data JPA repositories read and write structured entities such as patients, doctors, and appointments in MySQL.
6. For flexible or document-oriented information, the service layer uses MongoDB repositories to read and write document models, while coordinating results with relational data when a workflow needs both databases.
7. Results return from the repositories to the service layer and then to the requesting controller. MVC controllers add data to the model and select a Thymeleaf view for HTML rendering, while REST controllers serialize results into JSON and return an HTTP status and response body.
