# 10. Failures are codes, in four families

Status: accepted

## Context

The backend never produces text for users: the Angular app shows Dutch or French. The API must still say exactly what went wrong, with an HTTP status that fits.

## Decision

Every expected failure is a domain exception with a stable code, such as `SUBMISSION_AFTER_CLOSING_DATE`, which the frontend translates into Dutch or French. Each aggregate lists its codes in its own enum, next to the rules that throw them, rather than in one file every feature would touch.

The family says what kind of failure it is, and the API maps each to one HTTP status: not found (404), forbidden (403), business rule violated (400), and not allowed in the current status (409). The English message is for developers: it becomes the problem's `detail`, which clients never parse or show. A bug, such as corrupt stored data, is a plain Java exception and becomes a 500.

## Consequences

When someone may not even know an application exists, such as an applicant asking for someone else's, or staff asking for a draft, the answer is 404 rather than 403, so ids can't be probed.

A test fails the build when the contract doesn't name a code a client can receive. Not-found codes never reach a client, since they all become the same 404; they let the domain's tests and the API tell apart what's missing.
