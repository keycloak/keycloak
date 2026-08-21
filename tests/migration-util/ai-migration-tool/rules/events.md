# Event Assertion Migration Rules

Read this file when the legacy test uses `@Rule AssertEvents`, `@Rule AssertAdminEvents`, or event assertion patterns.

---

## Login Event Assertions

```java
// OLD
@Rule
public AssertEvents events = new AssertEvents(this);

events.expectLogin().user(userId).session(sessionId)
    .detail(Details.USERNAME, "user")
    .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
    .assertEvent();

events.expectCodeToToken(codeId, sessionId)
    .user(userId).session(sessionId).assertEvent();

events.clear();
events.assertEmpty();
```

```java
// NEW
@InjectEvents
Events events;

// Assert login event — poll() gets the oldest unprocessed event
EventAssertion.assertSuccess(events.poll())
    .type(EventType.LOGIN)
    .userId(userId)
    .sessionId(sessionId)
    .details(Details.USERNAME, "user")
    .details(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);

// Assert code-to-token event
EventAssertion.assertSuccess(events.poll())
    .type(EventType.CODE_TO_TOKEN)
    .userId(userId)
    .sessionId(sessionId);

events.clear();
```

**Imports:**
```
org.keycloak.testframework.annotations.InjectEvents
org.keycloak.testframework.events.Events
org.keycloak.testframework.events.EventAssertion
```

---

## EventAssertion API Reference

### Factory methods (static)

| Method | Purpose |
|---|---|
| `EventAssertion.assertSuccess(event)` | Assert event is a success (type does NOT end with `_ERROR`) |
| `EventAssertion.assertError(event)` | Assert event is an error (type ends with `_ERROR`) |

### Assertion chain methods (return `this`)

| Method | Purpose |
|---|---|
| `.type(EventType)` | Assert event type (e.g., `EventType.LOGIN`, `EventType.CODE_TO_TOKEN`) |
| `.userId(String)` | Assert user ID |
| `.sessionId(String)` | Assert session ID |
| `.clientId(String)` | Assert client ID |
| `.details(String key, String value)` | Assert a detail entry exists with given key/value |
| `.withoutDetails(String... keys)` | Assert detail keys are NOT present |
| `.error(String)` | Assert error message (for error events) |
| `.hasSessionId()` | Assert sessionId is set and is a valid UUID/base64 |
| `.hasIpAddress()` | Assert ipAddress is set and is localhost |
| `.isCodeId()` | Assert `code_id` detail is a valid code ID |

### Common migration mappings

| Old (AssertEvents) | New (EventAssertion) |
|---|---|
| `events.expectLogin()...assertEvent()` | `EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)` |
| `events.expectLogin().error(err)` | `EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(err)` |
| `events.expectCodeToToken(codeId, sessionId)` | `EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)` |
| `events.expectLogout(sessionId)` | `EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)` |
| `.user(userId).assertEvent()` | `.userId(userId)` |
| `.user(userRep).assertEvent()` | `.userId(userRep.getId())` — new API only accepts String, not UserRepresentation |
| `.session(sessionId).assertEvent()` | `.sessionId(sessionId)` |
| `.detail(key, value).assertEvent()` | `.details(key, value)` |
| `.removeDetail(key).assertEvent()` | `.withoutDetails(key)` |
| `events.clear()` | `events.clear()` |
| `events.assertEmpty()` | `events.skipAll()` or just don't poll |

---

## Events API Reference

The `Events` and `AdminEvents` classes both extend `AbstractEvents` and share these methods:

| Method | Purpose |
|---|---|
| `poll()` | Get the oldest unprocessed event. Fetches from server if queue is empty. |
| `skip()` | Skip the next event |
| `skip(int n)` | Skip the next N events |
| `skipAll()` | Skip all current events (resets the window) |
| `clear()` | Clear all events locally and on the server |

**Important**: `poll()` returns `null` if no events are available. Events are scoped to the current test method — events from previous tests are automatically ignored.

---

## Admin Event Assertions

```java
// OLD
@Rule
public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

assertAdminEvents.expectCreate(ResourceType.USER)
    .realm(realmId)
    .resourcePath(Matchers.containsString(userId))
    .assertEvent();

assertAdminEvents.expectUpdate(ResourceType.REALM)
    .realm(realmId)
    .representation(rep)
    .assertEvent();
```

```java
// NEW — Method chaining style (preferred)
@InjectAdminEvents
AdminEvents adminEvents;

AdminEventAssertion.assertSuccess(adminEvents.poll())
    .operationType(OperationType.CREATE)
    .resourceType(ResourceType.USER)
    .resourcePath("users", userId);

AdminEventAssertion.assertSuccess(adminEvents.poll())
    .operationType(OperationType.UPDATE)
    .resourceType(ResourceType.REALM)
    .representation(rep);

// NEW — Positional convenience method (legacy style, still available)
AdminEventAssertion.assertEvent(adminEvents.poll(),
    OperationType.CREATE,
    AdminEventPaths.userResourcePath(userId),
    ResourceType.USER);

// With representation
AdminEventAssertion.assertEvent(adminEvents.poll(),
    OperationType.UPDATE,
    AdminEventPaths.realmResourcePath(),
    rep,
    ResourceType.REALM);
```

**Imports:**
```
org.keycloak.testframework.annotations.InjectAdminEvents
org.keycloak.testframework.events.AdminEvents
org.keycloak.testframework.events.AdminEventAssertion
org.keycloak.tests.utils.admin.AdminEventPaths
```

---

## AdminEventAssertion API Reference

### Factory methods (static)

| Method | Purpose |
|---|---|
| `AdminEventAssertion.assertSuccess(event)` | Assert success event |
| `AdminEventAssertion.assertError(event)` | Assert error event |
| `AdminEventAssertion.assertEvent(event, opType, path, resourceType)` | Convenience: assert success + all fields at once |
| `AdminEventAssertion.assertEvent(event, opType, path, rep, resourceType)` | Convenience: same but with representation check |

### Assertion chain methods (return `this`)

| Method | Purpose |
|---|---|
| `.operationType(OperationType)` | Assert operation type (CREATE, UPDATE, DELETE, ACTION) |
| `.resourceType(ResourceType)` | Assert resource type (USER, REALM, CLIENT, etc.) |
| `.resourcePath(String... parts)` | Assert resource path (parts are joined with `/`) |
| `.representation(Object)` | Assert representation matches (reflection-based comparison of non-null fields) |
| `.auth(realmId, clientId, userId)` | Assert authentication details |

### Common migration mappings

| Old (AssertAdminEvents) | New (AdminEventAssertion) |
|---|---|
| `assertAdminEvents.expectCreate(ResourceType.X).assertEvent()` | `AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.CREATE).resourceType(ResourceType.X)` |
| `assertAdminEvents.expectUpdate(ResourceType.X).assertEvent()` | `...operationType(OperationType.UPDATE).resourceType(ResourceType.X)` |
| `assertAdminEvents.expectDelete(ResourceType.X).assertEvent()` | `...operationType(OperationType.DELETE).resourceType(ResourceType.X)` |
| `.realm(realmId)` | Realm is auto-checked by the framework |
| `.resourcePath(path)` | `.resourcePath(path)` |
| `.representation(rep)` | `.representation(rep)` |

---

## EventMatchers (Hamcrest matchers)

`EventMatchers` provides matchers for validating event field formats — NOT event types or clients:

| Matcher | Purpose |
|---|---|
| `EventMatchers.isUUID()` | Validates value is a UUID |
| `EventMatchers.isCodeId()` | Validates value is a code_id (UUID or base64) |
| `EventMatchers.isSessionId()` | Validates value is a session_id (UUID or base64) |

These are used internally by `EventAssertion.hasSessionId()`, `.isCodeId()`, etc.

---

## Critical: Always Check Existing Tests

The event assertion APIs differ significantly from the old framework. **Always** search for existing migrated tests that use `@InjectEvents` or `@InjectAdminEvents` in `tests/base/src/test/java/` and copy their patterns:

```bash
grep -rln "InjectEvents\|InjectAdminEvents" tests/base/src/test/java/
```

Read 2-3 of those tests to see the real usage patterns before writing event assertion code.
