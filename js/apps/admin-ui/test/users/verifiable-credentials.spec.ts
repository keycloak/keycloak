import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { expect, test } from "@playwright/test";
import { toUser } from "../../src/user/routes/User.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { skipIfOID4VCIFeatureDisabled } from "../utils/oid4vci.ts";
import { clickRowKebabItem, getRowByCellText } from "../utils/table.ts";

const CREDENTIAL_SCOPE_NAME = "employee-credential";
const USERNAME = "credential-user";

const realm: RealmRepresentation = {
  verifiableCredentialsEnabled: true,
  clientScopes: [
    {
      name: CREDENTIAL_SCOPE_NAME,
      protocol: "oid4vc",
    },
  ],
  users: [
    {
      username: USERNAME,
      firstName: "John",
      lastName: "Doe",
      email: "john.doe@example.com",
      enabled: true,
    },
  ],
};

test.describe("User verifiable credentials", () => {
  test.beforeEach(async () => {
    await skipIfOID4VCIFeatureDisabled();
  });

  test("creates, views, updates, and revokes a credential", async ({
    page,
  }) => {
    await using testBed = await createTestBed(realm);
    const user = await adminClient.findUserByUsername(testBed.realm, USERNAME);

    await login(page, {
      to: toUser({
        realm: testBed.realm,
        id: user.id!,
        tab: "verifiable-credentials",
      }),
    });

    await page
      .getByRole("button", { name: "Create verifiable credential" })
      .click();
    await selectItem(page, "#credentialScopeName", CREDENTIAL_SCOPE_NAME);
    await page.getByRole("button", { name: "Create", exact: true }).click();
    await assertNotificationMessage(
      page,
      "Verifiable credential successfully created.",
    );

    let row = getRowByCellText(page, CREDENTIAL_SCOPE_NAME);
    await expect(row).toBeVisible();
    const originalRevision = await row.locator("td").nth(1).innerText();

    await clickRowKebabItem(page, CREDENTIAL_SCOPE_NAME, "View attributes");
    let attributesDialog = page.getByRole("dialog", {
      name: `User attributes for "${CREDENTIAL_SCOPE_NAME}"`,
    });
    await expect(
      attributesDialog.getByRole("row", { name: /firstName John/ }),
    ).toBeVisible();
    await expect(
      attributesDialog.getByRole("row", { name: /lastName Doe/ }),
    ).toBeVisible();
    await attributesDialog.getByRole("button", { name: "Close" }).click();

    await adminClient.updateUser(user.id!, {
      realm: testBed.realm,
      firstName: "Jane",
    });

    await clickRowKebabItem(page, CREDENTIAL_SCOPE_NAME, "Update credential");
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Verifiable credential successfully updated.",
    );

    row = getRowByCellText(page, CREDENTIAL_SCOPE_NAME);
    await expect(row.locator("td").nth(1)).not.toHaveText(originalRevision);

    await clickRowKebabItem(page, CREDENTIAL_SCOPE_NAME, "View attributes");
    attributesDialog = page.getByRole("dialog", {
      name: `User attributes for "${CREDENTIAL_SCOPE_NAME}"`,
    });
    await expect(
      attributesDialog.getByRole("row", { name: /firstName Jane/ }),
    ).toBeVisible();
    await attributesDialog.getByRole("button", { name: "Close" }).click();

    await clickRowKebabItem(page, CREDENTIAL_SCOPE_NAME, "Revoke");
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Verifiable credential successfully revoked.",
    );
    await expect(getRowByCellText(page, CREDENTIAL_SCOPE_NAME)).toHaveCount(0);
  });
});
