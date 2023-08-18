import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { expect, test } from "@playwright/test";
import { importRealm, importUserProfile } from "../admin-client";
import userProfileRealm from "./personal-info-realm.json" assert { type: "json" };
import userProfileConfig from "./user-profile.json" assert { type: "json" };

test.describe("Personal info page", () => {
  test("sets basic information", async ({ page }) => {
    await page.goto("./");
    await page.getByTestId("email").fill("edewit@somewhere.com");
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    const alerts = page.getByTestId("alerts");
    await expect(alerts).toHaveText("Your account has been updated.");
  });
});

test.describe("Personal info with userprofile enabled", async () => {
  test.beforeAll(async () => {
    await importRealm(userProfileRealm as RealmRepresentation);
    await importUserProfile(
      userProfileConfig as UserProfileConfig,
      "user-profile",
    );
  });
});
