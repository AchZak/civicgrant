# 13. No message broker

Status: accepted

## Context

The domain's events feed the audit trail, the business metrics and the removal of a deleted document's file. One application consumes them, and no other system does.

## Decision

The events are handled in-process, by listeners in the same application. The audit trail writes its entry in the same transaction as the change. The business metrics, and the removal of a deleted document's file, wait until the transaction has committed.

## Consequences

A broker would add cost and moving parts without solving anything. I'd introduce Azure Service Bus with a transactional outbox the moment another system needs these events, for example a finance system that pays out approved grants.
