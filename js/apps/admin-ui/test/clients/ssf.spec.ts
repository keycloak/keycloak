import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import {
  assertClientSettingsLoaded,
  assertSsfNavigationTabsVisible,
  assertSsfPendingLookupVisible,
  assertSsfReceiverSaveButtonVisible,
  assertSsfStreamEmptyStateVisible,
  assertSsfSubjectTypeVisible,
  assertSsfTabHidden,
  loginToSsfTab,
  navigateToClientSettings,
  openSsfSubTab,
} from "./ssf.ts";

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

  test("shows the SSF tab and all sub-tabs for an opted-in client", async ({
    page,
  }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid: ssfClientUuid,
      tab: "receiver",
    });
    await assertSsfNavigationTabsVisible(page);
    await assertSsfReceiverSaveButtonVisible(page);
  });

  test("navigates between the SSF sub-tabs", async ({ page }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid: ssfClientUuid,
      tab: "receiver",
    });

    // The test client has no registered stream, so the Stream sub-tab shows
    // the "not registered" empty state rather than the refresh/stream card.
    await openSsfSubTab(page, "stream");
    await assertSsfStreamEmptyStateVisible(page);

    await openSsfSubTab(page, "subjects");
    await assertSsfSubjectTypeVisible(page);

    await openSsfSubTab(page, "event-search");
    await assertSsfPendingLookupVisible(page);

    await openSsfSubTab(page, "receiver");
    await assertSsfReceiverSaveButtonVisible(page);
  });

  test("hides the SSF tab for a client that has not opted in", async ({
    page,
  }) => {
    // First prove the SSF tab does render for the opted-in client — otherwise
    // the negative assertion below could pass vacuously (e.g. if the feature
    // were off, the tab would be absent for every client).
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid: ssfClientUuid,
      tab: "receiver",
    });

    // Then, without re-logging in (the session persists), open the client that
    // has NOT opted in and confirm the tab is absent.
    await navigateToClientSettings(page, realmName, plainClientUuid);
    await assertClientSettingsLoaded(page);
    await assertSsfTabHidden(page);
  });
});
