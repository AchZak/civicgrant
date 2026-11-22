# 49. Blob Storage has its own package in persistence

Status: accepted

## Context

The persistence module has one package per aggregate, like the domain. The supporting documents' files went into `persistence.grantapplication` with the rest of the grant application, because the `DocumentStorage` port belongs to that aggregate. The package then held fourteen classes on three technologies: the JPA entities, mapper and adapter, the JDBC reference sequence, and the Blob Storage adapter with its client configuration and settings. The Blob classes share nothing with the JPA ones except the rule that names a file, which the mapper also writes into each document's row.

## Decision

Blob Storage lives in `persistence.documentstorage`: `BlobDocumentStorage`, `DocumentBlobNames`, `DocumentStorageConfiguration` and `DocumentStorageProperties`. A package in persistence follows the aggregate for what PostgreSQL stores, and the outbound port for any other store.

`DocumentBlobNames` becomes public, since the grant application mapper stores the same name in the `blob_name` column. Apart from the generated metamodel, it's the only public class in the module, and ArchUnit's `persistence_classes_are_package_private` names it as its one exception.

## Alternatives considered

- Keeping the classes in `persistence.grantapplication`: no public class, but one package mixing three technologies, with the module's Blob client wiring next to JPA entities.
- Relaxing the ArchUnit rule to "nothing outside persistence uses a persistence class": no exception to name, but any class in the module could then turn public without the build noticing.

## Consequences

`persistence.grantapplication` holds only what PostgreSQL stores for an application. Another adapter that isn't the database gets its own package the same way, named after its port. The rule that names a blob stays in one place, now visible to the whole module.
