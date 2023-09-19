import { Page } from "@playwright/test";

export const login = async (
  page: Page,
  username: string,
  password: string,
  realm?: string,
) => {
  if (realm)
    await page.goto(
      process.env.CI ? `/realms/${realm}/account` : `/?realm=${realm}`,
    );
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
};
