import type { Page } from "@playwright/test";
import { generatePath, type Path } from "react-router-dom";

import {
  ADMIN_PASSWORD,
  ADMIN_USER,
  DEFAULT_REALM,
  ROOT_PATH,
  SERVER_URL,
} from "./constants.ts";

export type LoginOptions = {
  realm?: string;
  username?: string;
  password?: string;
  to?: Partial<Path>;
};

const DEFAULT_LOGIN_OPTIONS: Required<LoginOptions> = {
  realm: DEFAULT_REALM,
  username: ADMIN_USER,
  password: ADMIN_PASSWORD,
  to: {},
};

export async function login(
  page: Page,
  options: LoginOptions = {},
): Promise<void> {
  const { realm, username, password, to } = {
    ...DEFAULT_LOGIN_OPTIONS,
    ...options,
  };

  await navigateTo(page, to, realm);
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
}

export async function navigateTo(
  page: Page,
  to: Partial<Path>,
  rootRealm = DEFAULT_REALM,
): Promise<void> {
  const url = new URL(
    generatePath(ROOT_PATH, { realm: rootRealm }),
    SERVER_URL,
  );

  if (to.pathname) {
    url.hash = to.pathname;
  }

  await page.goto(url.toString());
}

export async function logout(page: Page, username = ADMIN_USER): Promise<void> {
  await page.getByRole("button", { name: username, exact: true }).click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
}
