import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import {
  assertSsfAudienceValue,
  assertSsfPollDeliveryMethodUnchecked,
  assertSsfPushAndPollDeliveryMethodsChecked,
  assertSsfPushDeliveryMethodChecked,
  assertSsfReceiverSaveButtonVisible,
  clickSsfReceiverRevert,
  clickSsfReceiverSave,
  disableSsfPollDeliveryMethod,
  fillSsfAudience,
  loginToSsfTab,
  navigateToSsfTab,
} from "./ssf.ts";

// Exercises the Receiver sub-tab of a client's SSF view — the primary
// configuration form (Save / Revert). The integration server is always
// started with the `ssf` feature (see
// js/apps/keycloak-server/scripts/start-server.js, #49977), so these tests
// assert the SSF tab renders and fail loudly if it is absent, rather than
// skipping.
test.describe.serial("Client SSF receiver", () => {
  const realmName = `ssf-receiver-realm-${uuid()}`;
  const clientId = `ssf-receiver-client-${uuid()}`;
  const audience = "https://receiver.example.com/ssf";

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

  // Runs first so the client is still pristine (audience empty), giving Revert
  // a deterministic baseline to restore to.
  test("reverts unsaved edits", async ({ page }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "receiver",
    });
    await assertSsfReceiverSaveButtonVisible(page);
    await assertSsfAudienceValue(page, "");

    await fillSsfAudience(page, "temporary-unsaved-value");
    await clickSsfReceiverRevert(page);
    await assertSsfAudienceValue(page, "");
  });

  test("saves receiver config and persists it across a reload", async ({
    page,
  }) => {
    await loginToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "receiver",
    });
    await assertSsfReceiverSaveButtonVisible(page);

    // A plain text field...
    await fillSsfAudience(page, audience);

    // ...and the custom delivery-method checkboxes, which drive the
    // ssf.allowedDeliveryMethods attribute. Both render checked on a fresh
    // receiver (unset = both allowed); unchecking poll should persist "push".
    await assertSsfPushAndPollDeliveryMethodsChecked(page);
    await disableSsfPollDeliveryMethod(page);
    await assertSsfPollDeliveryMethodUnchecked(page);

    await clickSsfReceiverSave(page);
    await assertNotificationMessage(page, "Client successfully updated");

    // Re-navigate from scratch (no re-login — the session persists, so
    // login() would hang waiting for a sign-in form that never appears) and
    // confirm the values round-tripped through storage and back into the form.
    await navigateToSsfTab(page, {
      realm: realmName,
      clientUuid,
      tab: "receiver",
    });
    await assertSsfReceiverSaveButtonVisible(page);
    await assertSsfAudienceValue(page, audience);
    await assertSsfPushDeliveryMethodChecked(page);
    await assertSsfPollDeliveryMethodUnchecked(page);
  });
});
