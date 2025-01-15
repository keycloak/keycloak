import { Page } from "@playwright/test";

import {
  ADMIN_PASSWORD,
  ADMIN_USER,
  DEFAULT_REALM,
  SERVER_URL,
  ROOT_PATH,
} from "./constants";

export const login = async (
  page: Page,
  username = ADMIN_USER,
  password = ADMIN_PASSWORD,
  realm = DEFAULT_REALM,
) => {
  const rootPath = SERVER_URL + ROOT_PATH.replace(":realm", realm);

  await page.goto(rootPath);
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
};

export const logout = async (page: Page, username: string = "admin") => {
  await page.getByRole("button", { name: username, exact: true }).click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
};
