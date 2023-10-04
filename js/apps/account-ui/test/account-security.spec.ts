import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("Account security page", () => {
  test("Check linked accounts available", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "photoz");
    await page.getByRole("button", { name: "Account security" }).click();
    expect(
      await (
        await page.$(
          'li.pf-c-nav__item[data-ouia-component-id="OUIA-Generated-NavItem-8"]',
        )
      )?.innerText(),
    ).toContain("Linked accounts");
  });
});
