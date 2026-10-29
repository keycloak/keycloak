import type UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { expect, type Page, test } from "@playwright/test";
import { assertIsDesktopView } from "../masthead/main.ts";
import adminClient from "../utils/AdminClient.ts";
import { createTestBed } from "../support/testbed.ts";
import { login, navigateTo } from "../utils/login.ts";

type RealmAdmin = { realm: string; username: string; password: string };

async function createRealmAdmin(realm: string): Promise<RealmAdmin> {
  const admin = { realm, username: "session-admin", password: "password" };
  const { id } = await adminClient.createUser({
    realm,
    username: admin.username,
    enabled: true,
    email: "session-admin@example.com",
    firstName: "Session",
    lastName: "Admin",
    credentials: [{ type: "password", value: admin.password }],
  });
  await adminClient.addClientRoleToUser(
    id!,
    "realm-management",
    ["realm-admin"],
    realm,
  );
  return admin;
}

async function loginWithSession(page: Page, admin: RealmAdmin) {
  const [response] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().endsWith("/protocol/openid-connect/token") &&
        response.request().method() === "POST",
    ),
    login(page, admin),
  ]);
  const tokenResponse = await response.json();
  expect(
    response.ok(),
    `Token request failed with status ${response.status()}: ${JSON.stringify(
      tokenResponse,
    )}`,
  ).toBe(true);
  const { id_token: idToken } = tokenResponse;
  expect(idToken, "Login must return an ID token").toBeTruthy();
  const session = JSON.parse(
    Buffer.from(idToken.split(".")[1], "base64url").toString(),
  ) as { sid: string; sub: string };
  expect(session.sub, "ID token must identify the user").toEqual(
    expect.any(String),
  );
  expect(session.sid, "ID token must identify the session").toEqual(
    expect.any(String),
  );
  return session;
}

async function signOutSession(
  page: Page,
  realm: string,
  userId: string,
  sessionId: string,
) {
  const sessionsResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/users/${userId}/sessions`) &&
      response.request().method() === "GET" &&
      response.ok(),
  );
  await navigateTo(
    page,
    { pathname: `/${realm}/users/${userId}/sessions` },
    realm,
  );
  const sessions: UserSessionRepresentation[] = await (
    await sessionsResponse
  ).json();
  const index = sessions.findIndex((session) => session.id === sessionId);
  expect(index).toBeGreaterThanOrEqual(0);
  const row = page.locator("table tbody tr").nth(index);
  await row.getByLabel("Kebab toggle").click();
  const deleted = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname.endsWith(`/sessions/${sessionId}`) &&
      response.request().method() === "DELETE",
  );
  await page.getByRole("menuitem", { name: "Sign out", exact: true }).click();
  expect((await deleted).status()).toBe(204);
}

test.describe("Sign out a specific session", () => {
  test("keeps the admin console authenticated when another browser session is removed", async ({
    page,
    browser,
  }) => {
    await using testBed = await createTestBed({ enabled: true });
    const admin = await createRealmAdmin(testBed.realm);
    const { sid, sub } = await loginWithSession(page, admin);

    const otherContext = await browser.newContext();
    try {
      const otherPage = await otherContext.newPage();
      const otherSession = await loginWithSession(otherPage, admin);
      expect(otherSession.sub).toBe(sub);
      expect(otherSession.sid).not.toBe(sid);

      await signOutSession(page, testBed.realm, sub, otherSession.sid);
      await assertIsDesktopView(page);
      const sessionsResponse = page.waitForResponse(
        (response) =>
          response.url().endsWith(`/users/${sub}/sessions`) && response.ok(),
      );
      await page.reload();
      await assertIsDesktopView(page);
      const sessions: UserSessionRepresentation[] = await (
        await sessionsResponse
      ).json();
      expect(sessions.some((session) => session.id === sid)).toBe(true);
      expect(sessions.some((session) => session.id === otherSession.sid)).toBe(
        false,
      );
    } finally {
      await otherContext.close();
    }
  });

  test("logs out the admin console when its current session is removed", async ({
    page,
  }) => {
    await using testBed = await createTestBed({ enabled: true });
    const admin = await createRealmAdmin(testBed.realm);
    const { sid, sub } = await loginWithSession(page, admin);

    await signOutSession(page, testBed.realm, sub, sid);

    await expect(page).toHaveURL(/\/auth/);
  });
});
