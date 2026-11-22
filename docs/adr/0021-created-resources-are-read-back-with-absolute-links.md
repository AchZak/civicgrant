# 21. A created resource is read back, and every link is absolute

Status: accepted

## Context

Belgif's guide asks for absolute URLs in every link the API returns. The Container Apps ingress ends HTTPS and passes plain HTTP to the app, so the app doesn't see the scheme and host the client used.

## Decision

After a create, the API reads the new resource back and returns it. The `Location` header, like every link the API returns, is an absolute URL.

The app takes the original scheme and host from the ingress's `X-Forwarded-*` headers, and trusts them only from private network addresses, where the ingress connects from. Spring Boot does this by itself once it recognises the platform, but the setting is explicit, so it doesn't depend on that detection.

## Consequences

The client sees what was stored, such as the trimmed name and the enterprise number without dots, not an echo of its own request. A client on the internet can't fake the forwarded headers. A test against a real server proves the links come out absolute and in HTTPS.
