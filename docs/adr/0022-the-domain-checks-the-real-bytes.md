# 22. The domain checks the real bytes, and uploads have a hard limit

Status: accepted

## Context

A supporting document must be a PDF of at most 10 MiB. A renamed image must be refused, and the size limit must hold before the app reads anything.

## Decision

The declared type must say PDF, and the content must start with the PDF signature. The whole file reaches the use case as bytes, so the domain checks size and signature itself, without any I/O.

Tomcat refuses a file over 10 MiB or a request over 11 MiB before any of the app's code runs, with Belgif's `payloadTooLarge` (413).

A download is sent as an attachment with the original file name, its exact length and `nosniff`.

## Alternatives considered

Passing a stream and its size would have saved memory, but then a domain service would read the first bytes, push them back and handle I/O errors.

## Consequences

An upload costs at most 10 MiB of memory, bounded by the server. Sonar flags any limit above 8 MB for review; this one is the product's rule, and uploads are only read once the caller is signed in.

A browser never tries to run a download as anything but a PDF.
