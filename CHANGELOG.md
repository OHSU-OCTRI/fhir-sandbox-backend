# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- Initial commit including code forked from [hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) (RFS-253)
- Added `RUNNING.md` with instructions on how to configure and run the application for development (RFS-253)

### Changed

- Disabled integration tests in GitHub Actions workflow (RFS-253)
- Configured GitHub Actions to cache Maven dependencies (RFS-253)
- Change `groupId` and `artifactId` in `pom.xml` (RFS-253)

### Removed

- Deleted most GitHub Actions workflows (RFS-253)


[unreleased]: https://github.com/OCTRI-Apps/fhir-sandbox-backend/compare/v0.1.0...HEAD
[0.1.0]: https://source.ohsu.edu/OCTRI-Apps/fhir-sandbox-backend/releases/tag/v0.1.0
