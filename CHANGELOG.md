# Changelog

## [UNRELEASED]

### Dependencies

- Bump `micrometer.version` from 1.16.2 to 1.16.5 ([#12](https://github.com/OHSU-OCTRI/fhir-sandbox-backend/pull/12))

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- Initial commit including code forked from [hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) (RFS-253)
- Added `RUNNING.md` with instructions on how to configure and run the application for development (RFS-255)
- Added initial implementation of the `/.well-known/smart-configuration` endpoint
- Added initial JWT authentication (RFS-250)
- Validate that JWT bearer token has access to the requested partition (RFS-250)

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

### Removed

- Deleted most GitHub Actions workflows (RFS-253)
- Deleted Maven build workflow (RFS-256)
- Delete build step for Tomcat image variant (RFS-256)


[unreleased]: https://github.com/OCTRI-Apps/fhir-sandbox-backend/compare/v0.1.0...HEAD
[0.1.0]: https://source.ohsu.edu/OCTRI-Apps/fhir-sandbox-backend/releases/tag/v0.1.0
