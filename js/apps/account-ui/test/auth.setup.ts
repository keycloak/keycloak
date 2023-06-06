import { test as setup } from "@playwright/test";

const authFile = ".auth/user.json";

setup("authenticate", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Personal info" }).click();
  const userName = page.getByLabel("Username or email");
  await userName.fill("admin");
  await page.getByLabel("Password").fill("admin");
  await page.getByRole("button", { name: "Sign In" }).click();
  await page.waitForURL("/#/personal-info");

  await page.context().storageState({ path: authFile });
});
