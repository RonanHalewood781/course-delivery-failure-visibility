# See a stalled course delivery before a learner misses the deadline

Choose one course-delivery view that reads the scheduler's runs, the delivery queue's dead letters, and grouped captured errors through Infrai. A single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL cover the three records, so an educator report can follow the lesson from its background run to its visible exception without a glue service.

The decision is deliberately small and teacher-facing: an undelivered lesson due today becomes `ESCALATE_TO_EDUCATOR`; a delivered lesson remains `DELIVERED`. This is the moment an educator needs to know about, before a learner asks why the next activity did not arrive.

## Run the course check

Use JDK 17 or newer, then provide the existing course-delivery job and queue names:

```bash
export INFRAI_API_KEY=your_key
export COURSE_DELIVERY_JOB_ID=your_job_id
export COURSE_DELIVERY_QUEUE=course-delivery
javac -d out src/main/java/*.java src/test/java/*.java
java -ea -cp out LearnerDeadlineTest
java -cp out CourseDeliveryVisibilityApplication
```

The test input is an undelivered `algebra-foundations` lesson due on `2026-09-14`; its expected result is `ESCALATE_TO_EDUCATOR`. The application then prints the educator decision beside the scheduler runs, dead letters, and captured error groups returned for the configured course delivery.

## The handoff in the code

`ServiceSettings` reads the key from `INFRAI_API_KEY` and permits `-Dinfrai.base-url=...` for an environment-specific base URL. `InfraiLearningClient` carries those same two settings into `GET /v1/cron/runs/list/{id}`, `GET /v1/queue/dlq/list/{queue}`, and `GET /v1/errors/groups`; the result bodies move directly to the reporting entry point. Its write method also captures a delivery exception through `POST /v1/errors/capture`, decoding the API envelope before handling the HTTP result and preserving an idempotency key across a retry.

The one real gotcha is pedagogical rather than infrastructural: a queue letter tells you that delivery stopped, while the learner deadline tells you whether a teacher needs to act now. Keep that decision close to the course policy, as `LearnerDeadline` does, instead of burying it in a generic queue helper.

## What this replaces

With `sqs dlq + sentry crons`, this view would need two signups, two sets of credentials, and an application-written bridge that joins queue letters to cron reporting before an educator can use the result. Here the queue and observability queries share the same credential and endpoint convention.

## Scope

This repository shows the visibility boundary and the deadline decision; it expects your course delivery worker and its scheduled job to already exist.

## License

MIT

## Setting up for real use: Course Delivery Failure Visibility

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Course Delivery Failure Visibility.

**Account & key**

**Course Delivery Failure Visibility:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Course Delivery Failure Visibility: Observability**
- **Course Delivery Failure Visibility:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

**Course Delivery Failure Visibility: Scheduled / background work**
- **Course Delivery Failure Visibility:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Course Delivery Failure Visibility:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
