# 24. A file and its row stay in step without a distributed transaction

Status: accepted

## Context

A supporting document is a row in PostgreSQL and a file in Blob Storage, and no transaction spans both.

## Decision

Attaching a document checks the rules, saves the application, which already writes the row and its audit entry to the database, and uploads the file last, still inside the transaction.

Removing a document deletes the row, and the file only after the transaction has committed.

## Consequences

A version conflict or a failed audit entry fails before any file exists, and a failed upload rolls back the row and its entry. A removal that's rolled back keeps its file. So a row never points to a missing file.

The worst case is a file without a row, after a failed commit or a failed delete, which is private, named by ids, and logged. Cleaning those up would be a scheduled job if it ever mattered.
