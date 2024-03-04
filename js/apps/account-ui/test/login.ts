import { Page } from "@playwright/test";
import { DEFAULT_REALM } from "../src/constants";
import { getRootPath } from "../src/utils/getRootPath";

export const login = async (
  page: Page,
  username: string,
  password: string,
  realm = DEFAULT_REALM,
) => {
  const rootPath = getRootPath(realm);

  await page.goto(rootPath);
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
};
