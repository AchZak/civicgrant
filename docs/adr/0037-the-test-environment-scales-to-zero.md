# 37. The test environment costs nothing while nobody uses it, and holds no key

Status: accepted

## Context

The test environment is idle most of the time, and should cost nothing then.

## Decision

The app runs on Azure Container Apps, in an environment with only the serverless Consumption profile, which has no fixed fee. It scales between zero and one replica on HTTP traffic, with half a vCPU and 1 GiB.

Logs and telemetry go to one Log Analytics workspace, capped well below the free monthly volume. Neither the workspace nor Application Insights accepts keys: the app sends telemetry with its own managed identity, and the platform's logs are routed by a diagnostic setting.

## Consequences

With no visitors nothing runs and nothing is billed, and the first visit waits for a cold start of under a minute. Even a busy month stays far inside the free monthly allowance.

The Application Insights connection string grants nothing on its own.
