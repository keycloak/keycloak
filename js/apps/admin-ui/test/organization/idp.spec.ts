import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import {
  addMapper,
  clickCancelMapper,
  clickSaveMapper,
  goToMappersTab,
} from "../identity-providers/main.ts";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import {
  goToIdentityProviders as goToRealmIdentityProviders,
  goToOrganizations,
  goToRealm,
} from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  clickTableRowItem,
} from "../utils/table.ts";
import {
  clickAddIdentityProvider,
  fillForm,
  goToIdentityProviders as goToOrganizationIdentityProviders,
} from "./idp.ts";

test.describe.serial("Identity providers", () => {
  const realmName = `organization-idp-${uuid()}`;
  const organizationName = "linked-idp";
  const foreignOrganizationName = "foreign-idp";
  const targetRoleName = `linked-role-${uuid()}`;
  const foreignRoleName = `foreign-role-${uuid()}`;
  const mapperName = `organization-role-mapper-${uuid()}`;
  let organizationId: string;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: organizationName,
      domains: [{ name: "o.com", verified: false }],
    });
    await adminClient.createOrganization({
      realm: realmName,
      name: foreignOrganizationName,
    });
    ({ organizationId } = await adminClient.createOrganizationRole(
      organizationName,
      { name: targetRoleName },
      realmName,
    ));
    await adminClient.createOrganizationRole(
      foreignOrganizationName,
      { name: foreignRoleName },
      realmName,
    );
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
    await clickTableRowItem(page, organizationName);
    await goToOrganizationIdentityProviders(page);
  });

  test("should link an identity provider and configure an organization role mapper", async ({
    page,
  }) => {
    await goToRealmIdentityProviders(page);
    await clickTableRowItem(page, "BitBucket");
    await goToMappersTab(page);
    await addMapper(page, "oidc-hardcoded-organization-role", mapperName);
    const unscopedRoleSelect = page
      .locator("#config\\.organizationRole")
      .getByRole("combobox");
    await expect(unscopedRoleSelect).toBeVisible();
    await unscopedRoleSelect.click();
    await unscopedRoleSelect.fill(targetRoleName);
    await expect(
      page.getByRole("option", { name: targetRoleName, exact: true }),
    ).toHaveCount(0);
    await clickCancelMapper(page);

    await goToOrganizations(page);
    await clickTableRowItem(page, organizationName);
    await goToOrganizationIdentityProviders(page);
    await assertEmptyTable(page);
    await clickAddIdentityProvider(page);
    await fillForm(page, { name: "bitbucket", domain: "o.com" });
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Identity provider successfully linked to organization",
    );
    await assertRowExists(page, "bitbucket");

    await clickTableRowItem(page, "bitbucket");
    await goToMappersTab(page);
    await addMapper(page, "oidc-hardcoded-organization-role", mapperName);

    await expect(
      page.getByText("Organization Role", { exact: true }),
    ).toBeVisible();
    const roleSelect = page
      .locator("#config\\.organizationRole")
      .getByRole("combobox");
    await expect(roleSelect).toBeEnabled();
    await roleSelect.click();

    const foreignSearchResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        url.pathname.endsWith(`/organizations/${organizationId}/roles`) &&
        url.searchParams.get("search") === foreignRoleName
      );
    });
    await roleSelect.fill(foreignRoleName);
    expect((await foreignSearchResponse).status()).toBe(200);
    await expect(
      page.getByRole("option", { name: foreignRoleName, exact: true }),
    ).toHaveCount(0);

    const targetSearchResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        url.pathname.endsWith(`/organizations/${organizationId}/roles`) &&
        url.searchParams.get("search") === targetRoleName &&
        url.searchParams.get("max") === "20"
      );
    });
    await roleSelect.fill(targetRoleName);
    expect((await targetSearchResponse).status()).toBe(200);
    await page
      .getByRole("option", { name: targetRoleName, exact: true })
      .click();
    await expect(roleSelect).toHaveValue(targetRoleName);

    await clickSaveMapper(page);
    await assertRowExists(page, mapperName);
    await clickTableRowItem(page, mapperName);
    await expect(
      page.locator("#config\\.organizationRole").getByRole("combobox"),
    ).toHaveValue(targetRoleName);
  });
});
