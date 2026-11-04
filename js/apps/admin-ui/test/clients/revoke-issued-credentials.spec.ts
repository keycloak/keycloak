import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toClient } from "../../src/clients/routes/Client.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import { login } from "../utils/login.ts";
import { skipIfOID4VCIFeatureDisabled } from "../utils/oid4vci.ts";

const CLIENT_ID = `wallet-client-${uuid()}`;

test.describe("Revoke issued credentials of a wallet client", () => {
  test.beforeEach(async () => {
    await skipIfOID4VCIFeatureDisabled();
  });

  test("revokes all issued credentials from the client actions", async ({
    page,
  }) => {
    await using testBed = await createTestBed({
      verifiableCredentialsEnabled: true,
    });

    const { id } = await adminClient.createClient({
      realm: testBed.realm,
      clientId: CLIENT_ID,
      attributes: { "oid4vci.enabled": "true" },
    });

    await login(page, {
      to: toClient({
        realm: testBed.realm,
        clientId: id,
        tab: "settings",
      }),
    });

    await page.getByTestId("action-dropdown").click();
    const revokeAction = page.getByRole("menuitem", {
      name: "Revoke all issued credentials",
      exact: true,
    });
    await expect(revokeAction).toBeVisible();
    await revokeAction.click();
    await assertModalTitle(page, "Revoke all issued credentials?");
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Issued credentials successfully revoked.",
    );
  });

  test("does not show the action for a non-wallet client", async ({ page }) => {
    await using testBed = await createTestBed({
      verifiableCredentialsEnabled: true,
    });

    const { id } = await adminClient.createClient({
      realm: testBed.realm,
      clientId: `regular-client-${uuid()}`,
    });

    await login(page, {
      to: toClient({
        realm: testBed.realm,
        clientId: id,
        tab: "settings",
      }),
    });

    await page.getByTestId("action-dropdown").click();
    await expect(
      page.getByRole("menuitem", {
        name: "Revoke all issued credentials",
        exact: true,
      }),
    ).toBeHidden();
  });
});
