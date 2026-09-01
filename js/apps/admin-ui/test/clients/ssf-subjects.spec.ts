import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import {
  addSsfSubject,
  assertSsfSubjectStatus,
  assertSsfSubjectTypeVisible,
  assertSsfSubjectValueError,
  checkSsfSubject,
  fillSsfSubjectValue,
  ignoreSsfSubject,
  loginToSsfTab,
  removeSsfSubject,
} from "./ssf.ts";

// Exercises the Subjects sub-tab of a client's SSF view: the per-subject
// add / ignore / remove / check actions that map to
// POST /admin/realms/{realm}/ssf/clients/{clientId}/subjects/{action}.
// The integration server is always started with the `ssf` feature (see
// js/apps/keycloak-server/scripts/start-server.js, #49977), so these tests
// assert the SSF tab renders and fail loudly if it is absent, rather than
// skipping.
test.describe.serial("Client SSF subjects", () => {
  const realmName = `ssf-subjects-realm-${uuid()}`;
  const clientId = `ssf-subjects-client-${uuid()}`;
  const userEmail = "ssf-subject@example.com";

  let clientUuid: string;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, {
      attributes: { "ssf.transmitterEnabled": "true" },
    });

    const client = await adminClient.createClient({
      realm: realmName,
      clientId,
      protocol: "openid-connect",
      publicClient: false,
      attributes: { "ssf.enabled": "true" },
    });
    clientUuid = client.id!;

    // A real user is required — the subject endpoints resolve the value to a
    // Keycloak user, and an unknown subject returns 404.
    await adminClient.createUser({
      realm: realmName,
      username: "ssf-subject-user",
      email: userEmail,
      emailVerified: true,
      enabled: true,
    });
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  test("requires a subject value", async ({ page }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "subjects",
    });
    await assertSsfSubjectTypeVisible(page);

    await checkSsfSubject(page);
    await assertSsfSubjectValueError(page, "Please enter a subject value.");
  });

  test("reports an unknown subject as not found", async ({ page }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "subjects",
    });
    await assertSsfSubjectTypeVisible(page);

    await fillSsfSubjectValue(page, "nobody@example.com");
    await checkSsfSubject(page);
    await assertSsfSubjectValueError(
      page,
      "Subject not found. Verify the value and subject type.",
    );
  });

  test("adds, checks, ignores, and removes a subject", async ({ page }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "subjects",
    });
    await assertSsfSubjectTypeVisible(page);
    await fillSsfSubjectValue(page, userEmail);

    // Check first: a brand-new subject is not yet part of event delivery.
    await checkSsfSubject(page);
    await assertSsfSubjectStatus(
      page,
      "Subject is not included in event delivery for this stream.",
    );

    // Add: success notification and the status flips to "notified".
    await addSsfSubject(page);
    await assertNotificationMessage(
      page,
      "Subject has been added to this stream.",
    );
    await assertSsfSubjectStatus(
      page,
      "Events for this subject are delivered to this stream.",
    );

    // Ignore: explicitly excludes the subject.
    await ignoreSsfSubject(page);
    await assertNotificationMessage(
      page,
      "Subject has been ignored for this stream.",
    );
    await assertSsfSubjectStatus(
      page,
      "Subject is explicitly excluded from event delivery for this stream.",
    );

    // Remove: clears the explicit entry, back to not-included.
    await removeSsfSubject(page);
    await assertNotificationMessage(
      page,
      "Subject has been removed from this stream.",
    );
    await assertSsfSubjectStatus(
      page,
      "Subject is not included in event delivery for this stream.",
    );
  });
});
