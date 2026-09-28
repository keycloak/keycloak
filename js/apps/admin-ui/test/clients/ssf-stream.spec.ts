import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import {
  assertCreateSsfStreamEndpointErrorHidden,
  assertCreateSsfStreamEndpointErrorVisible,
  assertCreateSsfStreamEndpointVisible,
  assertCreateSsfStreamSubmitDisabled,
  assertCreateSsfStreamSubmitEnabled,
  assertRegisteredSsfStreamView,
  assertSsfStreamEmptyStateVisible,
  fillCreateSsfStreamDescription,
  fillCreateSsfStreamEndpoint,
  loginToSsfTab,
  openCreateSsfStreamForm,
  submitCreateSsfStream,
} from "./ssf.ts";

// Exercises the admin-console "Create stream" flow on a client's SSF Stream
// sub-tab. The integration server is always started with the `ssf` feature
// (see js/apps/keycloak-server/scripts/start-server.js, #49977), so these
// tests assert the SSF tab renders and fail loudly if it is absent, rather
// than skipping.
test.describe.serial("Client SSF stream creation", () => {
  const realmName = `ssf-stream-realm-${uuid()}`;
  const clientId = `ssf-stream-client-${uuid()}`;
  const pushEndpoint = "https://receiver.example.com/ssf/push";

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
      attributes: {
        "ssf.enabled": "true",
        // The transmitter rejects a push stream whose endpoint isn't on the
        // receiver's push allowlist (ssf.validPushUrls), so seed it with a
        // pattern matching pushEndpoint — otherwise create-stream is rejected.
        "ssf.validPushUrls": "https://receiver.example.com/*",
      },
    });
    clientUuid = client.id!;
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  test("validates the push endpoint URL before allowing submit", async ({
    page,
  }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "stream",
    });

    // Open the create-stream form from the empty state.
    await assertSsfStreamEmptyStateVisible(page);
    await openCreateSsfStreamForm(page);

    // PUSH is the default delivery method, so the endpoint URL field is
    // shown. With it empty, the submit button stays disabled.
    await assertCreateSsfStreamEndpointVisible(page);
    await assertCreateSsfStreamSubmitDisabled(page);

    // A non-http(s) URL is rejected inline and submit remains disabled.
    await fillCreateSsfStreamEndpoint(page, "ftp://nope");
    await assertCreateSsfStreamEndpointErrorVisible(page);
    await assertCreateSsfStreamSubmitDisabled(page);

    // A valid https URL clears the error and enables submit.
    await fillCreateSsfStreamEndpoint(page, pushEndpoint);
    await assertCreateSsfStreamEndpointErrorHidden(page);
    await assertCreateSsfStreamSubmitEnabled(page);
  });

  test("creates a push stream", async ({ page }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "stream",
    });

    await openCreateSsfStreamForm(page);
    await fillCreateSsfStreamEndpoint(page, pushEndpoint);
    await fillCreateSsfStreamDescription(page, "Created by test");
    await submitCreateSsfStream(page);

    await assertNotificationMessage(page, "SSF stream created successfully.");

    // The empty state is replaced by the registered-stream view, which
    // surfaces the stream id and a refresh action.
    await assertRegisteredSsfStreamView(page);
  });
});
