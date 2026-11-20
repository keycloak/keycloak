import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { clickSaveButton } from "./main.ts";

const realmName = `idp-oauth2-defaults-${uuid()}`;
const oauth2ProviderName = "OAuth v2";

test.beforeAll(() => adminClient.createRealm(realmName));
test.afterAll(() => adminClient.deleteRealm(realmName));

test.describe("OAuth2 provider signature settings", () => {
  const alias = `oauth2-no-sig-${uuid()}`;

  test.afterAll(async () => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  });

  test("should not persist validateSignature when saving an OAuth2 provider", async ({
    page,
  }) => {
    // OAuth2 providers do not verify signatures, so the Admin Console must not
    // add a validateSignature entry to their configuration on save.
    await adminClient.createIdentityProvider(
      oauth2ProviderName,
      alias,
      realmName,
      {
        authorizationUrl: "https://idp.example/auth",
        tokenUrl: "https://idp.example/token",
        userInfoUrl: "https://idp.example/userinfo",
        clientId: "broker-client",
        clientSecret: "broker-secret",
      },
    );

    const before = await adminClient.getIdentityProvider(alias, realmName);
    expect(before?.config?.validateSignature).toBeUndefined();

    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oauth2/${alias}/settings`,
    );

    await page.getByTestId("displayName").fill(`OAuth2 ${alias}`);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");

    const after = await adminClient.getIdentityProvider(alias, realmName);
    expect(after?.displayName).toBe(`OAuth2 ${alias}`);
    expect(after?.config?.validateSignature).toBeUndefined();
  });
});
