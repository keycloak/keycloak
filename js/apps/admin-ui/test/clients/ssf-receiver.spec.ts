import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toSsfClientTab } from "../../src/clients/routes/ClientSsfTab.tsx";
import adminClient from "../utils/AdminClient.ts";
import { login, navigateTo } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";

// Exercises the Receiver sub-tab of a client's SSF view — the primary
// configuration form (Save / Revert). Like the other SSF specs this depends
// on the server `ssf` feature being enabled (see
// js/apps/keycloak-server/scripts/start-server.js); when it is off the SSF
// tab never renders and the feature-dependent tests skip.
test.describe.serial("Client SSF receiver", () => {
  const realmName = `ssf-receiver-realm-${uuid()}`;
  const clientId = `ssf-receiver-client-${uuid()}`;
  const audience = "https://receiver.example.com/ssf";

  // TextControl/TextAreaControl derive their data-testid from the form field
  // name, which is the beerified attribute path (dots after the first become
  // 🍺 — see convertAttributeNameToForm in src/util.ts).
  const audienceTestId = "attributes.ssf🍺streamAudience";

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
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  /**
   * Navigate to the client's SSF Receiver sub-tab (the default SSF sub-tab).
   * The integration server is always started with the `ssf` feature enabled
   * (see js/apps/keycloak-server/scripts/start-server.js, #49977), so the SSF
   * tab must render — assert it rather than skip, so a missing tab fails loudly.
   */
  async function goToReceiverTab(page: Page) {
    await login(page, {
      to: toSsfClientTab({
        realm: realmName,
        clientId: clientUuid,
        tab: "receiver",
      }),
    });

    await expect(page.getByTestId("ssfTab")).toBeVisible({ timeout: 15_000 });
    await expect(page.getByTestId("ssfReceiverSave")).toBeVisible();
  }

  // Runs first so the client is still pristine (audience empty), giving Revert
  // a deterministic baseline to restore to.
  test("reverts unsaved edits", async ({ page }) => {
    await goToReceiverTab(page);

    const audienceField = page.getByTestId(audienceTestId);
    await expect(audienceField).toHaveValue("");

    await audienceField.fill("temporary-unsaved-value");
    await page.getByTestId("ssfReceiverRevert").click();

    await expect(audienceField).toHaveValue("");
  });

  test("saves receiver config and persists it across a reload", async ({
    page,
  }) => {
    await goToReceiverTab(page);

    // A plain text field...
    await page.getByTestId(audienceTestId).fill(audience);

    // ...and the custom delivery-method checkboxes, which drive the
    // ssf.allowedDeliveryMethods attribute. Both render checked on a fresh
    // receiver (unset = both allowed); unchecking poll should persist "push".
    const pushCheckbox = page.getByTestId("ssfAllowedDeliveryMethods.push");
    const pollCheckbox = page.getByTestId("ssfAllowedDeliveryMethods.poll");
    await expect(pushCheckbox).toBeChecked();
    await expect(pollCheckbox).toBeChecked();
    await pollCheckbox.click();
    await expect(pollCheckbox).not.toBeChecked();

    await page.getByTestId("ssfReceiverSave").click();
    await assertNotificationMessage(page, "Client successfully updated");

    // Re-navigate from scratch (no re-login — the session persists, so
    // login() would hang waiting for a sign-in form that never appears) and
    // confirm the values round-tripped through storage and back into the form.
    await navigateTo(
      page,
      toSsfClientTab({
        realm: realmName,
        clientId: clientUuid,
        tab: "receiver",
      }),
    );
    await expect(page.getByTestId("ssfReceiverSave")).toBeVisible();
    await expect(page.getByTestId(audienceTestId)).toHaveValue(audience);
    await expect(
      page.getByTestId("ssfAllowedDeliveryMethods.push"),
    ).toBeChecked();
    await expect(
      page.getByTestId("ssfAllowedDeliveryMethods.poll"),
    ).not.toBeChecked();
  });
});
