# ether-http-core

Reusable HTTP contracts and primitives for Ether.

## Scope

- Transport-agnostic contracts (`HttpExchange`, `HttpResource`)
- Routing model (`Route`, `RouteMatcher`)
- Error mapping contracts (`HttpError`, `ErrorMapper`)
- Generic auth/middleware contracts
- Query language support (`QuerySpec`, `RSQL parser`)

This module must not depend on Jetty APIs.
