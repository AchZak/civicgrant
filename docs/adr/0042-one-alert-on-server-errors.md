# 42. One alert, on server errors only

Status: accepted

## Context

Application Insights' failed requests include every 4xx. A 403 or a 409 is the API refusing something by design, while a 500 is always a bug.

## Decision

Each environment has one alert rule, which emails within about five minutes when the app has answered with a 500 or a 503 in the last 15. Every environment notifies one shared action group, whose address is set at deploy time, like the budget's, and never stored in the repository.

## Consequences

Watching two status codes keeps the rule inside Azure Monitor's ten free time series.

An alert is only proven when it fires, so I deployed a temporary copy watching 401 and sent one request without a token: it fired after about five and a half minutes, resolved by itself about 25 minutes later, and was then deleted.

Azure delivers nothing to an email address until it's confirmed with a one-time code, which expires 30 minutes after the action group is saved, and no API shows whether that happened: the test email failing is what gave it away.
