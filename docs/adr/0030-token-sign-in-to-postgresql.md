# 30. The app signs in to PostgreSQL with a token, through Microsoft's JDBC plugin

Status: accepted

## Context

Password authentication is off on the server ([ADR 28](0028-one-postgresql-server-for-all-environments.md)), so the app has to sign in with a Microsoft Entra token from its managed identity.

## Decision

On Azure, the datasource URL names Microsoft's authentication plugin. For each new connection, the plugin asks the app's managed identity, chosen by its client id, for a token and hands it to the PostgreSQL driver as the password. The same URL makes the driver check the server's certificate and host name (`verify-full`) against the Java runtime's own trust store. Locally and in tests the URL doesn't name the plugin.

The Azure SDK's default HTTP transport, Netty, is replaced by the JDK's built-in HTTP client.

## Alternatives considered

Spring Cloud Azure would have set this up from a single property, but its current releases support Spring Boot 4.0, not the 4.1 this project runs. The plugin is what Spring Cloud Azure uses underneath, so switching later changes configuration, not behaviour.

## Consequences

The Java runtime's trust store already contains Azure's root certificates, so the image carries no certificate file. Docker Compose and Testcontainers keep signing in with a password.

There's one networking stack fewer in the image, and fewer security updates to follow. A test fails the build if Netty comes back.
