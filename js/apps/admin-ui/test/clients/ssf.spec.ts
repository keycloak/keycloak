import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toClient } from "../utils/routes.ts";
import { toSsfClientTab } from "../utils/routes.ts";
import adminClient from "../utils/AdminClient.ts";
import { login, navigateTo } from "../utils/login.ts";

// The SSF (Shared Signals Framework) client view is triple-gated in
// ClientDetails: the server `ssf` feature must be enabled, the realm must
// have `ssf.transmitterEnabled=true`, and the client must opt in with
// `ssf.enabled=true`. This suite wires up the realm- and client-level
// attributes.
//
// The integration server is always started with the `ssf` feature in the
// `--features` list of js/apps/keycloak-server/scripts/start-server.js
// (#49977), so these tests assume it is enabled and assert the SSF tab
// renders — a missing tab is a real failure, not an expected skip.
test.describe.serial("Client SSF tab", () => {
  const realmName = `ssf-realm-${uuid()}`;
  const ssfClientId = `ssf-client-${uuid()}`;
  const plainClientId = `plain-client-${uuid()}`;

  // Internal (UUID) ids, needed to build the deep-link client routes.
  let ssfClientUuid: string;
  let plainClientUuid: string;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, {
      // Realm-level transmitter opt-in.
      attributes: { "ssf.transmitterEnabled": "true" },
    });

    // Confidential OIDC client that has opted into being an SSF receiver.
    // allowEmitEvents=true additionally reveals the Emit Events sub-tab
    // (gated by showSsfEmitEventsTab in ClientDetails).
    const ssfClient = await adminClient.createClient({
      realm: realmName,
      clientId: ssfClientId,
      protocol: "openid-connect",
      publicClient: false,
      attributes: { "ssf.enabled": "true", "ssf.allowEmitEvents": "true" },
    });
    ssfClientUuid = ssfClient.id!;

    // Confidential OIDC client that has NOT opted in — used to assert the
    // tab stays hidden.
    const plainClient = await adminClient.createClient({
      realm: realmName,
      clientId: plainClientId,
      protocol: "openid-connect",
      publicClient: false,
    });
    plainClientUuid = plainClient.id!;
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  /**
   * Navigate straight to a client's SSF sub-tab via deep link and assert the
   * SSF tab rendered. The server always has the `ssf` feature enabled (see
   * file header), so a missing tab is a real failure, not a skip.
   */
  async function goToSsfTab(
    page: Page,
    tab: Parameters<typeof toSsfClientTab>[0]["tab"],
  ) {
    await login(page, {
      to: toSsfClientTab({ realm: realmName, clientId: ssfClientUuid, tab }),
    });

    // Use a generous timeout here (not the expect default) to absorb the
    // login + SPA load on the first navigation of the run.
    await expect(page.getByTestId("ssfTab")).toBeVisible({ timeout: 15_000 });
  }

  test("shows the SSF tab and all sub-tabs for an opted-in client", async ({
    page,
  }) => {
    await goToSsfTab(page, "receiver");

    await expect(page.getByTestId("ssfTab")).toBeVisible();
    await expect(page.getByTestId("ssfReceiverTab")).toBeVisible();
    await expect(page.getByTestId("ssfStreamTab")).toBeVisible();
    await expect(page.getByTestId("ssfSubjectsTab")).toBeVisible();
    await expect(page.getByTestId("ssfEventSearchTab")).toBeVisible();
    await expect(page.getByTestId("ssfEmitEventsTab")).toBeVisible();

    // The Receiver sub-tab is the default landing page; its save action
    // confirms the form rendered.
    await expect(page.getByTestId("ssfReceiverSave")).toBeVisible();
  });

  test("navigates between the SSF sub-tabs", async ({ page }) => {
    await goToSsfTab(page, "receiver");

    // The test client has no registered stream, so the Stream sub-tab shows
    // the "not registered" empty state rather than the refresh/stream card.
    await page.getByTestId("ssfStreamTab").click();
    await expect(page.getByTestId("empty-state")).toBeVisible();

    await page.getByTestId("ssfSubjectsTab").click();
    await expect(page.getByTestId("ssfSubjectType")).toBeVisible();

    await page.getByTestId("ssfEventSearchTab").click();
    await expect(page.getByTestId("ssfPendingLookup")).toBeVisible();

    await page.getByTestId("ssfReceiverTab").click();
    await expect(page.getByTestId("ssfReceiverSave")).toBeVisible();
  });

  test("hides the SSF tab for a client that has not opted in", async ({
    page,
  }) => {
    // First prove the SSF tab does render for the opted-in client — otherwise
    // the negative assertion below could pass vacuously (e.g. if the feature
    // were off, the tab would be absent for every client).
    await goToSsfTab(page, "receiver");
    await expect(page.getByTestId("ssfTab")).toBeVisible();

    // Then, without re-logging in (the session persists), open the client that
    // has NOT opted in and confirm the tab is absent.
    await navigateTo(
      page,
      toClient({
        realm: realmName,
        clientId: plainClientUuid,
        tab: "settings",
      }),
    );

    await expect(page.getByTestId("clientId")).toBeVisible();
    await expect(page.getByTestId("ssfTab")).toHaveCount(0);
  });
});
