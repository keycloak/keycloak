import type { Page } from "@playwright/test";
import { DEFAULT_PASSWORD, DEFAULT_USERNAME, getAccountUrl } from "./common.ts";

export async function login(
  page: Page,
  realm: string,
  username = DEFAULT_USERNAME,
  password = DEFAULT_PASSWORD,
): Promise<void> {
  await page.goto(getAccountUrl(realm).toString());
  await page
    .getByRole("textbox", { name: "Username or email", exact: true })
    .fill(username);
  await page
    .getByRole("textbox", { name: "Password", exact: true })
    .fill(password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
}
