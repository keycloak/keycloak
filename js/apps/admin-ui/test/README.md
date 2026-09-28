# Playwright Test Style Guide

This document describes how integration tests under `js/apps/admin-ui/test` should be written.

## Core principle

Tests should read like a user scenario. Prefer high-level action helpers over inline selector logic.

Use this style:

```ts
await login(page, { to: toAddUser({ realm: testBed.realm }) });

await fillUserForm(page, { username: "test-user" });
await joinGroup(page, ["test-group"]);
await clickSaveButton(page);
await assertNotificationMessage(page, "The user has been created");
```

The `*.spec.ts` file should communicate intent. Low-level Playwright details should live in helper files.

## File layout and naming

- Keep test files in `*.spec.ts`.
- Add companion helper files in the same folder (for example `main.ts`, `users.ts`, `flow.ts`).
- Keep helper names action-oriented and explicit:
  - `login`, `goToUsers`, `fillUserForm`, `joinGroup`, `clickSaveButton`
  - `assertNotificationMessage`, `assertRowExists`
- Use these prefixes consistently:
  - `goTo*` for navigation
  - `fill*` for form entry
  - `select*` / `toggle*` / `click*` for user actions
  - `assert*` for checks

## What goes in specs vs helpers

- `*.spec.ts` should contain:
  - test setup
  - scenario flow
  - assertions that explain expected behavior
- Helper `*.ts` files should contain:
  - selectors and locator details
  - reusable UI actions
  - reusable assertions
- Avoid embedding repeated selectors in specs. If a step appears in more than one test, extract it.

## Navigation and setup conventions

- Prefer route helpers and explicit destinations in login:
  - `await login(page, { to: toUsers({ realm: testBed.realm }) });`
- For isolated realm data, prefer `createTestBed()` and `await using` so cleanup is automatic:
  - `await using testBed = await createTestBed({ ...overrides });`
- Keep test data local to each test so scenarios are easy to understand.

## Selector conventions

- Prefer resilient selectors in helpers:
  - first choice: `getByTestId`
  - second choice: semantic queries (`getByRole`, `getByLabel`)
- Avoid brittle selectors (deep CSS chains, text-dependent selectors that are not stable).
- Keep selector changes localized to helper files to reduce churn.

## Assertions and stability

- Assert user-visible outcomes after meaningful actions (for example success notifications, row presence, URL change).
- Prefer explicit helper assertions such as `assertNotificationMessage(...)`.
- Rely on Playwright auto-waiting and expectations instead of manual sleeps.
- Do not use `waitForTimeout()` unless there is no better technical option and the reason is documented.

## Test design checklist

- Can the spec be read top-to-bottom as a scenario?
- Are action/assertion helper names describing intent clearly?
- Are selectors hidden in helpers?
- Is setup isolated and cleanup automatic?
- Are assertions validating behavior that matters to users?
