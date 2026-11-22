# 23. Documents live in Blob Storage, named by ids and reached with the app's identity

Status: accepted

## Context

Applicants upload PDFs, which must stay private. A storage name must carry no personal data, and a file name an applicant chose must never lead to another file.

## Decision

One storage account sits with the shared resources, next to the database, with one private container per environment. Each environment's app identity may read and write its own container and nothing else. Shared keys are off. Deleted files and containers stay recoverable for seven days, and a delete lock protects the account.

A file is stored as `applications/{applicationId}/{documentId}.pdf`. The name the applicant gave it stays in the database as information, never in a path.

Locally and in tests, Azurite, Microsoft's emulator, stands in for Blob Storage.

## Consequences

On Azure no connection string exists that could leak, and nothing is ever public. A file name can't be used to reach another file and puts no personal data in storage names. At the test environment's volumes the account costs well under a cent a month.
