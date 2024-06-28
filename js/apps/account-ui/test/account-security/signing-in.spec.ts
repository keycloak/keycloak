import { expect, test } from "@playwright/test";
import {
  getUserByUsername,
  getCredentials,
  deleteCredential,
  deleteRealm,
  importRealm,
} from "../admin-client";
import { login } from "../login";
import groupsRealm from "../realms/groups-realm.json" assert { type: "json" };
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

const realm = "groups";
test.describe("Signing in", () => {
  // Tests for keycloak account console, section Signing in in Account security
  test("Should see only password", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "groups");

    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    page.getByTestId("account-security/signing-in").click();

    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toContainText("My password");
    await expect(page.getByTestId("password/create")).toBeHidden();

    await expect(
      page.getByTestId("otp/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("otp/credential-list").getByRole("listitem"),
    ).toContainText("not set up");
    await expect(page.getByTestId("otp/create")).toBeVisible();

    await page.getByTestId("otp/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Mobile Authenticator Setup",
    );
  });
});

test.describe("Signing in 2", () => {
  test.afterAll(async () => {
    await deleteRealm(realm);
    await importRealm(groupsRealm as RealmRepresentation);
  });
  test("Password removal", async ({ page }) => {
    const jdoeUser = await getUserByUsername("jdoe", realm);

    await login(page, "jdoe", "jdoe", "groups");

    const credentials = await getCredentials(jdoeUser!.id!, realm);
    deleteCredential(jdoeUser!.id!, credentials![0].id!, realm);

    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    page.getByTestId("account-security/signing-in").click();

    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("password/credential-list").getByRole("listitem"),
    ).toContainText("not set up");
    await expect(page.getByTestId("password/create")).toBeVisible();

    await expect(
      page.getByTestId("otp/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("otp/credential-list").getByRole("listitem"),
    ).toContainText("not set up");
    await expect(page.getByTestId("otp/create")).toBeVisible();

    await page.getByTestId("password/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Update password",
    );
  });
});
