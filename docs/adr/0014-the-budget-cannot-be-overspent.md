# 14. The budget can't be overspent, even by two decision makers at once

Status: accepted

## Context

Two decision makers can approve applications of the same call at the same moment. If both wrote their own total, one grant would silently disappear from the budget.

## Decision

A funding call keeps the amount already committed. An approval first checks the application's own rules, such as who may decide and whether the amount stays within what was requested, and only then takes the grant from what's left of the call's budget. The application and the call are saved in the same transaction. The call has a version, like an application.

## Alternatives considered

Locking the call's row while deciding would also work, but every other decision on that call would then have to wait.

## Consequences

A refused approval never touches the budget. When two approvals read the call at the same moment, the second one finds the version changed when it saves, answers 409 `concurrentUpdate` and changes nothing, and the client reads the call again and retries.
