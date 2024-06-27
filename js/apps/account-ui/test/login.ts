import { Page } from "@playwright/test";

import { ADMIN_PASSWORD, ADMIN_USER, DEFAULT_REALM } from "./constants";
import { getRootPath } from "./utils";

export const login = async (
  page: Page,
  username = ADMIN_USER,
  password = ADMIN_PASSWORD,
  realm = DEFAULT_REALM,
  queryParams?: Record<string, string>,
) => {
  const rootPath =
    getRootPath(realm) +
    (queryParams ? "?" + new URLSearchParams(queryParams) : "");

  await page.goto(rootPath);
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
};
