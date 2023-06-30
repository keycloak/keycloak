import { Page } from "@playwright/test";

export const login = async (page: Page, username: string, password: string) => {
  await page.getByLabel("Username or email").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
};
