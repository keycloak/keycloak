import { test as setup } from "@playwright/test";
import { login } from "./login";

const authFile = ".auth/user.json";

setup("authenticate", async ({ page }) => {
  await page.goto("/");
  await login(page, "admin", "admin");
  await page.waitForURL("/");

  await page.context().storageState({ path: authFile });
});
