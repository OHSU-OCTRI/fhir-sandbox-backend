# Release Workflow

This document describes how to release the OCTRI authentication library (AuthLib). This will:

* Advance the version in the `pom.xml` files
* Add the new version to `CHANGELOG.md`
* Tag the new version and create a GitHub release with notes sourced from `CHANGELOG.md`
* Build release artifacts and deploy them to GitHub Packages and Maven Central

You must have write access to the repository to make a release.

## Before Releasing

You should do the following before releasing the library.

1. Verify that the "Unreleased" section of [`CHANGELOG.md`](./CHANGELOG.md) is up to date. Make a PR for any missing changes.
2. If any breaking changes are listed in `CHANGELOG.md`, they should also be documented in [`UPGRADING.md`](./UPGRADING.md).
3. Based on the changes included in `CHANGELOG.md`, use [semantic versioning (semver)](https://semver.org/) to determine the version that will be released.

## Releasing the Application

To release the application:

1. Select [the "Create release" workflow](https://github.com/OHSU-OCTRI/fhir-sandbox-backend/actions/workflows/release.yaml) on the Actions tab.
2. Open the "Run workflow" dropdown.
3. Fill in the form with the following values:
   - Select to use the workflow from the `main` branch.
   - Enter the version to release in the "Version to release" field.
   - Enter the next patch version in the "Next version to set..." field. For example, if releasing version X.Y.0, the next version should be X.Y.1.
4. Click "Run workflow" to make the release.

The release workflow automatically runs the [build workflow](https://github.com/OHSU-OCTRI/fhir-sandbox-backend/actions/workflows/build.yaml) to build and publish artifacts for the release.
