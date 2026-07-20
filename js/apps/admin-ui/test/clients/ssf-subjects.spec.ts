import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toSsfClientTab } from "../utils/routes.ts";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";

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

  /**
   * Navigate to the client's SSF Subjects sub-tab. The integration server is
   * always started with the `ssf` feature enabled (see
   * js/apps/keycloak-server/scripts/start-server.js, #49977), so the SSF tab
   * must render — assert it rather than skip, so a missing tab fails loudly.
   */
  async function goToSubjectsTab(page: Page) {
    await login(page, {
      to: toSsfClientTab({
        realm: realmName,
        clientId: clientUuid,
        tab: "subjects",
      }),
    });

    await expect(page.getByTestId("ssfTab")).toBeVisible({ timeout: 15_000 });

    // Default subject type is "By email", so the value field takes an email.
    await expect(page.getByTestId("ssfSubjectType")).toBeVisible();
  }

  test("requires a subject value", async ({ page }) => {
    await goToSubjectsTab(page);

    await page.getByTestId("ssfSubjectCheck").click();
    await expect(page.getByTestId("ssfSubjectValueError")).toHaveText(
      "Please enter a subject value.",
    );
  });

  test("reports an unknown subject as not found", async ({ page }) => {
    await goToSubjectsTab(page);

    await page.getByTestId("ssfSubjectValue").fill("nobody@example.com");
    await page.getByTestId("ssfSubjectCheck").click();
    await expect(page.getByTestId("ssfSubjectValueError")).toHaveText(
      "Subject not found. Verify the value and subject type.",
    );
  });

  test("adds, checks, ignores, and removes a subject", async ({ page }) => {
    await goToSubjectsTab(page);

    const value = page.getByTestId("ssfSubjectValue");
    const status = page.getByTestId("ssfSubjectStatus");
    await value.fill(userEmail);

    // Check first: a brand-new subject is not yet part of event delivery.
    await page.getByTestId("ssfSubjectCheck").click();
    await expect(status).toHaveText(
      "Subject is not included in event delivery for this stream.",
    );

    // Add: success notification and the status flips to "notified".
    await page.getByTestId("ssfSubjectAdd").click();
    await assertNotificationMessage(
      page,
      "Subject has been added to this stream.",
    );
    await expect(status).toHaveText(
      "Events for this subject are delivered to this stream.",
    );

    // Ignore: explicitly excludes the subject.
    await page.getByTestId("ssfSubjectIgnore").click();
    await assertNotificationMessage(
      page,
      "Subject has been ignored for this stream.",
    );
    await expect(status).toHaveText(
      "Subject is explicitly excluded from event delivery for this stream.",
    );

    // Remove: clears the explicit entry, back to not-included.
    await page.getByTestId("ssfSubjectRemove").click();
    await assertNotificationMessage(
      page,
      "Subject has been removed from this stream.",
    );
    await expect(status).toHaveText(
      "Subject is not included in event delivery for this stream.",
    );
  });
});
