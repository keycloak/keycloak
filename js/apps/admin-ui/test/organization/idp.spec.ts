import { expect, test } from "@playwright/test";
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
    await fillForm(page, { name: "bitbucket" });
    const linkRequest = page.waitForRequest(
      (request) =>
        request.method() === "POST" &&
        new URL(request.url()).pathname.endsWith("/identity-providers"),
    );
    await confirmModal(page);
    const request = await linkRequest;
    expect(request.headers()["content-type"]).toContain("application/json");
    expect(request.postDataJSON()).toBe("bitbucket");
    await assertNotificationMessage(
      page,
      "Identity provider successfully linked to organization",
    );
    await assertRowExists(page, "bitbucket");
  });
});
