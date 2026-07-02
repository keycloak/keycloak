import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toClient } from "../../src/clients/routes/Client.tsx";
import { toSsfClientTab } from "../../src/clients/routes/ClientSsfTab.tsx";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";

// The SSF (Shared Signals Framework) client view is triple-gated in
// ClientDetails: the server `ssf` feature must be enabled, the realm must
// have `ssf.transmitterEnabled=true`, and the client must opt in with
// `ssf.enabled=true`. This suite wires up the realm- and client-level
// attributes; the server feature is the remaining factor.
//
// NOTE: the dev/integration server only exposes SSF when `ssf` is present in
// the `--features` list of js/apps/keycloak-server/scripts/start-server.js
// (added by #49977). When that feature is off the SSF tab never renders, so
// the feature-dependent tests below skip themselves with a pointer rather
// than failing spuriously.
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
    const ssfClient = await adminClient.createClient({
      realm: realmName,
      clientId: ssfClientId,
      protocol: "openid-connect",
      publicClient: false,
      attributes: { "ssf.enabled": "true" },
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
   * Navigate straight to a client's SSF sub-tab via deep link, then skip the
   * test when the SSF tab is absent (server `ssf` feature disabled).
   */
  async function goToSsfTab(
    page: Page,
    tab: Parameters<typeof toSsfClientTab>[0]["tab"],
  ) {
    await login(page, {
      to: toSsfClientTab({ realm: realmName, clientId: ssfClientUuid, tab }),
    });

    // Wait for the client details page to finish rendering before deciding
    // whether the SSF tab is present — `count()` does not auto-wait, so
    // sampling it right after login() races the SPA load and would always
    // read 0. When the server `ssf` feature is off the tab never appears and
    // this times out, which we treat as "feature disabled" and skip.
    const featureEnabled = await page
      .getByTestId("ssfTab")
      .waitFor({ state: "visible", timeout: 15_000 })
      .then(() => true)
      .catch(() => false);
    test.skip(
      !featureEnabled,
      "SSF server feature is disabled — add 'ssf' to --features in js/apps/keycloak-server/scripts/start-server.js (#49977).",
    );
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
    await login(page, {
      to: toClient({
        realm: realmName,
        clientId: plainClientUuid,
        tab: "settings",
      }),
    });

    await expect(page.getByTestId("clientId")).toBeVisible();
    await expect(page.getByTestId("ssfTab")).toHaveCount(0);
  });
});
