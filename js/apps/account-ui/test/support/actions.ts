import { expect, type Page } from "@playwright/test";
import { DEFAULT_PASSWORD, DEFAULT_USERNAME, getAccountUrl } from "./common.ts";

export async function login(
  page: Page,
  realm: string,
  username = DEFAULT_USERNAME,
  password = DEFAULT_PASSWORD,
  queryParams?: URLSearchParams,
): Promise<void> {
  const url = getAccountUrl(realm);

  if (queryParams) {
    for (const [key, value] of queryParams) {
      url.searchParams.set(key, value);
    }
  }

  await page.goto(url.toString());
  await page
    .getByRole("textbox", { name: "Username or email", exact: true })
    .fill(username);
  await page
    .getByRole("textbox", { name: "Password", exact: true })
    .fill(password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
}

export async function assertLastAlert(
  page: Page,
  message: string,
): Promise<void> {
  await expect(page.getByTestId("last-alert")).toHaveText(message);
  await page
    .getByTestId("last-alert")
    .getByRole("button", { name: "Close alert", exact: false })
    .click();
}
