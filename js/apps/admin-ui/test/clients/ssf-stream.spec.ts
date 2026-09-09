import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toSsfClientTab } from "../../src/clients/routes/ClientSsfTab.tsx";
import adminClient from "../utils/AdminClient.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";

// Exercises the admin-console "Create stream" flow on a client's SSF Stream
// sub-tab. The integration server is always started with the `ssf` feature
// (see js/apps/keycloak-server/scripts/start-server.js, #49977), so these
// tests assert the SSF tab renders and fail loudly if it is absent, rather
// than skipping.
test.describe.serial("Client SSF stream creation", () => {
  const realmName = `ssf-stream-realm-${uuid()}`;
  const clientId = `ssf-stream-client-${uuid()}`;
  const pollClientId = `ssf-stream-poll-client-${uuid()}`;
  const pushEndpoint = "https://receiver.example.com/ssf/push";

  let clientUuid: string;
  let pollClientUuid: string;

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

    // Second receiver for the POLL-delivery flow. Poll streams need no
    // push allowlist — the transmitter hosts the poll endpoint itself.
    const pollClient = await adminClient.createClient({
      realm: realmName,
      clientId: pollClientId,
      protocol: "openid-connect",
      publicClient: false,
      attributes: { "ssf.enabled": "true" },
    });
    pollClientUuid = pollClient.id!;
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  /**
   * Navigate to the client's SSF Stream sub-tab. The integration server is
   * always started with the `ssf` feature enabled (see
   * js/apps/keycloak-server/scripts/start-server.js, #49977), so the SSF tab
   * must render — assert it rather than skip, so a missing tab fails loudly.
   */
  async function goToStreamTab(page: Page, client: string = clientUuid) {
    await login(page, {
      to: toSsfClientTab({
        realm: realmName,
        clientId: client,
        tab: "stream",
      }),
    });

    await expect(page.getByTestId("ssfTab")).toBeVisible({ timeout: 15_000 });
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
    await expect(page.getByTestId("endpointUrl-helper")).toBeVisible();
    await expect(submit).toBeDisabled();

    // A valid https URL clears the error and enables submit.
    await page.getByTestId("ssfCreateStreamEndpointUrl").fill(pushEndpoint);
    await expect(page.getByTestId("endpointUrl-helper")).toBeHidden();
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

    // "Last poll" is POLL-only state — a PUSH stream never polls, so the
    // field must not be rendered at all (not merely empty).
    await expect(page.getByTestId("ssfStreamLastVerifiedAt")).toBeVisible();
    await expect(page.getByTestId("ssfStreamLastPollCompletedAt")).toBeHidden();
  });

  test("creates a poll stream and shows it as never polled", async ({
    page,
  }) => {
    await goToStreamTab(page, pollClientUuid);

    await expect(page.getByTestId("empty-state")).toBeVisible();
    await page.getByRole("button", { name: "Create stream" }).click();

    // Switching to POLL hides the push endpoint field; nothing else is
    // required, so submit becomes enabled right away.
    await selectItem(page, "#ssfCreateStreamDeliveryMethod", "Poll");
    await expect(page.getByTestId("ssfCreateStreamEndpointUrl")).toBeHidden();
    await expect(page.getByTestId("ssfCreateStreamSubmit")).toBeEnabled();

    await page
      .getByTestId("ssfCreateStreamDescription")
      .fill("Poll stream created by test");
    await page.getByTestId("ssfCreateStreamSubmit").click();

    await assertNotificationMessage(page, "SSF stream created successfully.");
    await expect(page.getByTestId("ssfRefresh")).toBeVisible();

    // Fresh POLL stream: the field is rendered with the "never" marker.
    await expect(page.getByTestId("ssfStreamLastPollCompletedAt")).toHaveValue(
      "Never polled",
    );
  });

  test("formats the recorded last poll timestamp", async ({ page }) => {
    // The transmitter stamps ssf.stream.lastPollCompletedAt (epoch
    // seconds) on the receiver client when it serves a poll. Seed the
    // attribute directly so the test doesn't need a polling receiver.
    const lastPollCompletedAt = 1_760_000_000;
    await adminClient.updateClient(pollClientUuid, {
      realm: realmName,
      attributes: {
        "ssf.stream.lastPollCompletedAt": String(lastPollCompletedAt),
      },
    });

    await goToStreamTab(page, pollClientUuid);
    await expect(page.getByTestId("ssfRefresh")).toBeVisible();

    // The tab renders the value via Date#toLocaleString with the admin
    // user's locale ("en" by default). Compute the expectation inside
    // the same browser so Intl data and timezone match exactly.
    const expected = await page.evaluate(
      (seconds) => new Date(seconds * 1000).toLocaleString("en"),
      lastPollCompletedAt,
    );
    await expect(page.getByTestId("ssfStreamLastPollCompletedAt")).toHaveValue(
      expected,
    );
  });
});
