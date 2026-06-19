import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toSsfClientTab } from "../../src/clients/routes/ClientSsfTab.tsx";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";

// Exercises the admin-console "Create stream" flow on a client's SSF Stream
// sub-tab. Like ssf.spec.ts this depends on the server `ssf` feature being
// enabled (see js/apps/keycloak-server/scripts/start-server.js); when it is
// off the SSF tab never renders and the tests skip themselves.
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

  /**
   * Navigate to the client's SSF Stream sub-tab, skipping when the server
   * `ssf` feature is disabled (the tab never renders in that case). Uses
   * waitFor rather than count() so it doesn't race the SPA load.
   */
  async function goToStreamTab(page: Page) {
    await login(page, {
      to: toSsfClientTab({
        realm: realmName,
        clientId: clientUuid,
        tab: "stream",
      }),
    });

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

  test("validates the push endpoint URL before allowing submit", async ({
    page,
  }) => {
    await goToStreamTab(page);

    // Open the create-stream form from the empty state.
    await expect(page.getByTestId("empty-state")).toBeVisible();
    await page.getByRole("button", { name: "Create stream" }).click();

    // PUSH is the default delivery method, so the endpoint URL field is
    // shown. With it empty, the submit button stays disabled.
    const submit = page.getByTestId("ssfCreateStreamSubmit");
    await expect(page.getByTestId("ssfCreateStreamEndpointUrl")).toBeVisible();
    await expect(submit).toBeDisabled();

    // A non-http(s) URL is rejected inline and submit remains disabled.
    await page.getByTestId("ssfCreateStreamEndpointUrl").fill("ftp://nope");
    await expect(
      page.getByTestId("ssfCreateStreamEndpointUrlError"),
    ).toBeVisible();
    await expect(submit).toBeDisabled();

    // A valid https URL clears the error and enables submit.
    await page.getByTestId("ssfCreateStreamEndpointUrl").fill(pushEndpoint);
    await expect(
      page.getByTestId("ssfCreateStreamEndpointUrlError"),
    ).toBeHidden();
    await expect(submit).toBeEnabled();
  });

  test("creates a push stream", async ({ page }) => {
    await goToStreamTab(page);

    await page.getByRole("button", { name: "Create stream" }).click();
    await page.getByTestId("ssfCreateStreamEndpointUrl").fill(pushEndpoint);
    await page
      .getByTestId("ssfCreateStreamDescription")
      .fill("Created by test");
    await page.getByTestId("ssfCreateStreamSubmit").click();

    await assertNotificationMessage(page, "SSF stream created successfully.");

    // The empty state is replaced by the registered-stream view, which
    // surfaces the stream id and a refresh action.
    await expect(page.getByTestId("ssfRefresh")).toBeVisible();
    await expect(page.getByTestId("empty-state")).toBeHidden();
  });
});
