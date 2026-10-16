/**
 * Outbound persistence adapters: JPA entities and repositories for
 * PostgreSQL, and later the Blob Storage adapter for documents.
 * Implements the repository ports defined in the domain.
 *
 * <p>Owns the database schema through the Flyway migrations in
 * {@code src/main/resources/db/migration}.
 */
package be.achzak.civicgrant.persistence;
