import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToOrganizations, goToRealm } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  clickTableRowItem,
} from "../utils/table.ts";
import {
  clickAddIdentityProvider,
  clickUnlinkIdentityProvider,
  fillForm,
  goToIdentityProviders,
} from "./idp.ts";

test.describe.serial("Identity providers", () => {
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

test.describe.serial("Identity providers - multi domain", () => {
  const realmName = `org-idp-domains-${uuid()}`;
  const orgName = "multi-domain-org";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: orgName,
      domains: [
        { name: "alpha.com", verified: false },
        { name: "beta.com", verified: false },
        { name: "gamma.com", verified: false },
      ],
    });
    await adminClient.createIdentityProvider("BitBucket", "bitbucket", realmName);
    const orgId = await adminClient.findOrg(orgName, realmName);
    await adminClient.linkIdpToOrg(orgId, "bitbucket", realmName);
    const idp = await adminClient.getIdentityProvider("bitbucket", realmName);
    await adminClient.updateOrgDomain(orgId, "alpha.com", { idpId: idp!.internalId }, realmName);
    await adminClient.updateOrgDomain(orgId, "beta.com", { idpId: idp!.internalId }, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToIdentityProviders(page);
  });

  test("unlink multi domain idp and verify idp list is empty", async ({ page }) => {
    await assertRowExists(page, "bitbucket");
    await clickUnlinkIdentityProvider(page, "bitbucket");
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Identity provider has been unlinked",
    );
    await assertEmptyTable(page);
  });
});
