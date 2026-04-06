# Running the HAPI FHIR Server for Development

This application provides the multi-tenant FHIR back end for the OCTRI FHIR Sandbox application. See the following repository for the application that manages sandboxes, SMART on FHIR clients, and OAuth 2.0 authorization.

https://github.com/OHSU-OCTRI/fhir-sandbox

## Configuration

Copy `env.sample` to `.env` and update as desired. The default `.env` values set the PostgreSQL database credentials to match `docker-compose.yml` and set the server port to 8000 to avoid a conflict with the sandbox manager application.

```
# Customize the port to avoid conflict with the front end application
SERVER_PORT=8000

# Database configuration - these are the defaults used in docker-compose.yml
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hapi
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin
SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect

# FHIR server configuration
HAPI_FHIR_FHIR_VERSION=R4

# FHIR server UI configuration
HAPI_FHIR_TESTER_HOME_NAME=Local Tester
HAPI_FHIR_TESTER_SERVER_ADDRESS=http://localhost:8000/fhir
HAPI_FHIR_TESTER_HOME_REFUSE_TO_FETCH_THIRD_PARTY_URLS=false
HAPI_FHIR_TESTER_HOME_FHIR_VERSION=R4

# OAuth 2 authorization server configuration
OCTRI_SANDBOX_OAUTH2_AUTHORIZE_ADDRESS=http://localhost:8080/fhir-sandbox/oauth2/authorize
OCTRI_SANDBOX_OAUTH2_TOKEN_ADDRESS=http://localhost:8080/fhir-sandbox/oauth2/token
OCTRI_SANDBOX_OAUTH2_JWK_SET_ADDRESS=http://localhost:8080/fhir-sandbox/oauth2/jwks
```

Alternatively, you can create a `dev.yaml` file in `src/main/resources` and configure the properties there.

```yaml
server:
  port: 8000

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hapi
    username: admin
    password: admin
    driver-class-name: org.postgresql.Driver

  jpa:
    properties:
      hibernate:
        dialect: ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect

hapi:
  fhir:
    fhir_version: R4
    tester:
      home:
        name: Local Tester
        server_address: 'http://localhost:8000/fhir'
        refuse_to_fetch_third_party_urls: false
        fhir_version: R4

octri:
  sandbox:
    oauth2:
      authorize-address: http://localhost:8080/fhir-sandbox/oauth2/authorize
      token-address: http://localhost:8080/fhir-sandbox/oauth2/token
      jwk-set-address: http://localhost:8080/fhir-sandbox/oauth2/jwks
```

## Starting the Application

Start the PostgreSQL database using `docker compose`.

```bash
docker compose up -d hapi-fhir-postgres
```

Start the FHIR server using Visual Studio Code (e.g. using the Spring Boot Dashboard) or `mvn spring-boot:run`.

You can verify that the server is running by fetching the FHIR metadata using curl.

```bash
curl http://localhost:8000/fhir/DEFAULT/metadata
```
