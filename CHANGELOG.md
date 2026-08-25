# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Rewrite README.md to better reflect the changes made since forking from hapifhir/hapi-fhir-jpaserver-starter. (RFS-321)
- Update config and documentation to remove OHSU-specific configuration and clarify setup for new users. (RFS-321)

### Removed

- Remove OHSU-specific deployment files. (RFS-321)

## [0.3.0] - 2026-07-28

### Fixed

- Explicitly allow reading the `fhirUser` resource when scope and launch context ID are present (RFS-309)
- Add special handling for `patient/*` scopes for resources outside of the patient compartment (RFS-309)

## [0.2.0] - 2026-07-13

### Fixed

- Stub Kubernetes manifests for staging to fix error updating manifests during release (CIS-3779)

## [0.1.0] - 2026-07-13

### Added

- Initial commit including code forked from [hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) (RFS-253)
- Added `RUNNING.md` with instructions on how to configure and run the application for development (RFS-255)
- Added initial implementation of the `/.well-known/smart-configuration` endpoint
- Added initial JWT authentication (RFS-250)
- Validate that JWT bearer token has access to the requested partition (RFS-250)
- Authorize access using SMART scopes included in the JSON web token (RFS-293)
- Add missing information to SMART configuration response (RFS-302)

### Changed

- Disabled integration tests in GitHub Actions workflow (RFS-253)
- Configured GitHub Actions to cache Maven dependencies (RFS-253)
- Change `groupId` and `artifactId` in `pom.xml` (RFS-253)
- Enable partitioning by default (RFS-253)
- Use OCTRI configuration conventions (RFS-253)
- Ensure unit tests work when `dev.yaml` is present (RFS-253)
- Remap PostgreSQL port to avoid conflict with existing applications (RFS-253)
- Remap other ports and container names to avoid conflicts with existing applications (RFS-253)
- Update PostgreSQL driver to resolve vulnerability (RFS-256)
- Use OCTRI build and release workflows (RFS-256)
- Use OCTRI's usual image build pattern instead of multi-stage build (RFS-256)
- Add Kubernetes deployment manifests (RFS-256)
- Update package version in pom.xml to prevent package conflict (RFS-256)
- Suppress logging from `BaseInterceptorService` to prevent alerts for authn/authz failures (RFS-299)
- Update Kubernetes manifests for production deployment (RFS-297)
- Create a test build workflow to handle PRs (CIS-3773)

### Fixed

- Allow metadata requests without an `Authorization` header (RFS-299)
- Use FHIR sandbox base URL for audience check instead of full URL with query string (RFS-300)
- Add `IAuthRule` that authorizes transactions if any other rules are generated (RFS-304)
- Allow Patient$everything requests if the user has the correct scopes (RFS-310)
- Override inherited values in pom.xml to fix release issue (CIS-3779)

### Removed

- Deleted most GitHub Actions workflows (RFS-253)
- Deleted Maven build workflow (RFS-256)
- Delete build step for Tomcat image variant (RFS-256)

### Dependencies

- Bump `micrometer.version` from 1.16.2 to 1.16.5 ([#12](https://github.com/OHSU-OCTRI/fhir-sandbox-backend/pull/12))

[unreleased]: https://github.com/OHSU-OCTRI/fhir-sandbox-backend/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/OHSU-OCTRI/fhir-sandbox-backend/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/OHSU-OCTRI/fhir-sandbox-backend/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/OHSU-OCTRI/fhir-sandbox-backend/compare/4660cfbad3ecba599db212826fab3392c150e6bc...v0.1.0
