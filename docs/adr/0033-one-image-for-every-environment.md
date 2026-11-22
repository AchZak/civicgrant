# 33. One image for every environment

Status: accepted

## Context

An image rebuilt for each environment isn't the image that was tested.

## Decision

GitHub Actions builds one image per commit and promotes it through the environments. The image contains the application only. Everything that differs between environments, such as the database, the identity and the monitoring target, is set as environment variables on each Container App.

## Consequences

The image that passed acceptance is exactly the one that goes to production. There are no configuration files per environment in the application.
