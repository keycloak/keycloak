import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { confirmModal } from "../utils/modal";
import { goToOrganizations, goToRealm } from "../utils/sidebar";
import {
  assertEmptyTable,
  assertRowExists,
  clickTableRowItem,
} from "../utils/table";
import {
  clickAddIdentityProvider,
  fillForm,
  goToIdentityProviders,
} from "./idp";

test.describe("Identity providers", () => {
  const realmName = `organization-idp-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: "idp",
      domains: [{ name: "o.com", verified: false }],
    });
    await adminClient.createIdentityProvider(
      "BitBucket",
      "bitbucket",
      realmName,
    );
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, "idp");
    await goToIdentityProviders(page);
  });

  test("should add idp", async ({ page }) => {
    await assertEmptyTable(page);
    await clickAddIdentityProvider(page);
    await fillForm(page, { name: "bitbucket", domain: "o.com" });
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Identity provider successfully linked to organization",
    );
    await assertRowExists(page, "bitbucket");
  });
});
