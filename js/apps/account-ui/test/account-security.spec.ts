import { expect, test } from "@playwright/test";
import userProfileRealm from "./realms/user-profile-realm.json" with { type: "json" };
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";

test.describe("Linked accounts", () => {
  test("shows linked accounts", async ({ page }) => {
    const realm = await createTestBed(userProfileRealm);

    // Log in and navigate to the linked accounts section.
    await login(page, realm);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/linked-accounts").click();
    await expect(page.getByTestId("page-heading")).toHaveText(
      "Linked accounts",
    );
  });
});
