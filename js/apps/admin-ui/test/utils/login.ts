import type { Cookie, Page } from "@playwright/test";
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
  /** Signs in with the form instead of reusing the session of the worker, for tests that need their own session. */
  freshSession?: boolean;
};

const DEFAULT_LOGIN_OPTIONS: Required<LoginOptions> = {
  realm: DEFAULT_REALM,
  username: ADMIN_USER,
  password: ADMIN_PASSWORD,
  to: {},
  freshSession: false,
};

// Every worker is a separate process, so each worker signs in once and keeps its own SSO session.
// Later logins of the default admin reuse its cookies instead of filling in the login form.
let adminSessionCookies: Cookie[] | undefined;

export async function login(
  page: Page,
  options: LoginOptions = {},
): Promise<void> {
  const { realm, username, password, to, freshSession } = {
    ...DEFAULT_LOGIN_OPTIONS,
    ...options,
  };
  const reuseSession =
    !freshSession &&
    realm === DEFAULT_REALM &&
    username === ADMIN_USER &&
    password === ADMIN_PASSWORD;

  if (reuseSession && adminSessionCookies) {
    await page.context().addCookies(adminSessionCookies);
  }

  await navigateTo(page, to, realm);

  const usernameField = page.getByLabel("Username");
  const signInButton = page.getByRole("button", { name: "Sign In" });
  const masthead = page.getByRole("button", { name: username, exact: true });

  if (reuseSession) {
    // The cookies are rejected once the session has ended, then the login form is shown again.
    await signInButton.or(masthead).first().waitFor();
  }

  if (!reuseSession || (await signInButton.isVisible())) {
    await usernameField.fill(username);
    await page.getByLabel("Password", { exact: true }).fill(password);
    await signInButton.click();
  }

  if (reuseSession) {
    await masthead.waitFor();
    adminSessionCookies = await page.context().cookies();
  }
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
  // Wait for the redirect to finish, otherwise it can override a navigation that the caller starts next
  await page.getByRole("button", { name: "Sign In" }).waitFor();
}
