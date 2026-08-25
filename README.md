# FHIR Sandbox Backend

This project is a multi-tenant HAPI FHIR JPA server that provides the FHIR back end for the [OCTRI FHIR Sandbox application](https://github.com/OHSU-OCTRI/fhir-sandbox). It is forked from [hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) with authorization and multi-tenancy added.

See [OHSU-OCTRI/fhir-sandbox](https://github.com/OHSU-OCTRI/fhir-sandbox) for the application that manages sandboxes, SMART on FHIR clients, and OAuth 2.0 authorization.

## Development Info

This is a [Spring Boot](https://spring.io/projects/spring-boot) application built with Java 17 and Maven. It uses PostgreSQL for storage, run locally via `docker-compose.yml`.

## Setup

### Running for Local Development

See [RUNNING.md](./RUNNING.md) for instructions on how to configure and run the application for local development.

## Authentication and Authorization

Every inbound FHIR request is validated by a JWT bearer token, and SMART on FHIR scopes in the token are translated into HAPI FHIR authorization rules. See [docs/AUTH.md](./docs/AUTH.md) for a full description of the authorization pipeline and error codes.

## Multi-tenancy

The server uses FHIR partitioning to give each sandbox its own isolated data partition, identified by a UUID, in addition to the `DEFAULT` partition used for administrative requests. A request's JWT audience must match the partition it targets; see [docs/AUTH.md](./docs/AUTH.md) for details.

## Database Configuration (PostgreSQL)

The application is configured to use PostgreSQL by default. `docker-compose.yml` provides a PostgreSQL container with the following defaults, matched by `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: 'jdbc:postgresql://localhost:5432/hapi'
    username: admin
    password: admin
    driverClassName: org.postgresql.Driver
  jpa:
    properties:
      hibernate.dialect: ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect
```

See [RUNNING.md](./RUNNING.md) for how to override these values for local development using `.env` or `dev.yaml`.

## Further Configuration

For other HAPI FHIR server options not covered here (alternate databases, binary storage, Elasticsearch, MDM, CDS Hooks, Clinical Reasoning, subscriptions, custom interceptors/operations, etc.), see the [hapi-fhir-jpaserver-starter README](https://github.com/hapifhir/hapi-fhir-jpaserver-starter#readme) and the [HAPI FHIR documentation](https://hapifhir.io/hapi-fhir/docs/).

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for a history of changes to this project.
