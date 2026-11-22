# 38. Every push to develop deploys itself, through a Deployment Stack

Status: accepted

## Context

An environment should always match what's in Git, and nobody should delete its resources by hand. A deployment should only count as done when the new version is actually running.

## Decision

The "Deploy to test" job logs in as the environment's deploy identity, writes a preview of the changes to the run's summary, and applies the templates as a Deployment Stack. The stack deletes any resource removed from the templates, and a deny assignment stops anyone, Owner included, from deleting its resources by hand.

The job only goes green when the app reports healthy, which includes its token sign-in to the database, and when the new revision is the one serving.

Values shared by every environment are repository variables. The deploy identity's and the app identity's ids are variables of the GitHub Environment, which take precedence in the jobs that use it.

## Consequences

The environment always matches Git. A revision that never becomes ready leaves the previous one running, so a health check alone could pass against the old image; checking which revision serves catches that.
