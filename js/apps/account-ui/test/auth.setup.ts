import { test as setup } from "@playwright/test";
import { login } from "./login";
import { useTheme } from "./admin-client";

const authFile = ".auth/user.json";

setup("authenticate", async ({ page }) => {
  useTheme();
  await page.goto("./");
  await login(page, "admin", "admin");
  await page.waitForURL("./");

  await page.context().storageState({ path: authFile });
});
