# Authentication and Authorization

## Overview

Every inbound FHIR request passes through `JwtAuthorizationInterceptor`, which validates a JWT bearer token and converts SMART on FHIR scopes into HAPI FHIR authorization rules. The pipeline has four stages:

1. token extraction
1. token validation
1. audience check
1. scope-to-rule translation

The `/metadata` (capability statement) endpoint is always allowed without a token.

## Stage 1 — Token Extraction

The interceptor reads the `Authorization` header. The value must start with `Bearer ` and be followed by a non-blank token string. Any other value (missing header, wrong prefix, blank token) returns status code 401 with error code `9991`.

## Stage 2 — Token Validation

The token is processed by a `JWTProcessor` (configured elsewhere). If the token is expired, malformed, or has an invalid signature, the request is rejected with status code 401 and error code `9992`.

## Stage 3 — Audience Check

The token's `aud` claim is compared against the incoming request URL. Every audience value must be a valid FHIR partition root URL of the form `{scheme}://{host}/fhir/{partition}/`, where `{partition}` is either DEFAULT or a UUID. If an audience does not match the expected pattern, the request is rejected with status code 401 and error code `9997`.

## Stage 4 — Scope Processing

### Scope Parsing

The `scope` claim is expected to be an array of SMART scope strings. Both SMART V1 and V2 formats are accepted. V1 permissions are normalized to V2 during parsing:

| V1 | V2 equivalent |
|----|---------------|
| `.read` | `.rs` |
| `.write` | `.cud` |
| `.*` | `.cruds` |

V2 permission characters must be a non-empty, ordered (no duplicates) subset of `cruds`.

Non-resource scopes (`openid`, `fhirUser`, `profile`, `launch`, `launch/*`, `offline_access`, `online_access`) are recognized and ignored for authorization rule purposes.

Resource scope format: `{context}/{ResourceType}.{permissions}[?queryString]`

- **context**: must be `patient`, `user`, or `system`
- **ResourceType**: must be `*` (wildcard) or start with an uppercase letter (FHIR convention)
- **permissions**: subset of `c` (create) `r` (read) `u` (update) `d` (delete) `s` (search)
- **queryString**: optional filter hint; currently ignored by the rule builder (HAPI FHIR compartment rules do not enforce them)

A blank or structurally invalid scope returns status code 401 with error code `9994`.

### Launch Context

For `patient/`-context scopes, the token must also contain a `launchContext` claim. This claim is deserialized into a `LaunchContext` object, which carries:

| Field | Meaning |
|-------|---------|
| `patient` | FHIR Patient ID for compartment restriction |
| `encounter` | Encounter ID (available but not currently used for compartment rules) |
| `fhirUser` | FHIR ID of the authenticated user |
| `clientId` | Registered OAuth client ID |

Note that patient and encounter IDs should be a bare ID string, not a FHIR reference such as `Patient/abcd123`. On the other hand, the `fhirUser` ID should be a reference, because it could represent a `Patient`, `Practitioner`, `PractitionerRole`, `RelatedPerson`, or `Person`.

A missing or invalid `launchContext` claim returns status code 401 with error code `9995`. A `patient/` scope with a `launchContext` that has no `patient` field returns status code 401 with error code `9996`.

### Rule Translation

Each resource scope is translated to one or more HAPI FHIR `IAuthRule` objects:

| Permission char | HAPI FHIR operation |
|-----------------|---------------------|
| `r` or `s` | read (covers both read-by-ID and search) |
| `c` | create |
| `u` | write (update) |
| `d` | delete |

Compartment restriction depends on scope context:

| Scope context | Compartment applied |
|---------------|---------------------|
| `patient/` | `inCompartment("Patient", <patientId from launchContext>)` |
| `user/` | `withAnyId()` (no compartment restriction) |
| `system/` | `withAnyId()` (no compartment restriction) |

A wildcard resource type (`*`) maps to HAPI FHIR's `allResources()`; a named resource type maps to `resourcesOfType(<name>)`.

If the combined rule list is empty after processing all scopes, a catch-all deny rule is applied.

## Error Code Reference

| Code | HTTP | Meaning |
|------|------|---------|
| 9991 | 401 | Missing or invalid `Authorization` header |
| 9992 | 401 | Invalid or expired bearer token |
| 9993 | 403 | Request URL not covered by token audience |
| 9994 | 401 | Scope claim is missing, empty, or contains an invalid scope |
| 9995 | 401 | `launchContext` claim is missing or cannot be parsed |
| 9996 | 401 | `patient/` scope present but no patient ID in launch context |
| 9997 | 401 | Token audience value does not match the expected format |
