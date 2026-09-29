import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";

test.describe("Realm settings quick theme", () => {
  const realmName = `quick-theme-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async () => {
    const enabled = await adminClient.isFeatureEnabled("QUICK_THEME");
    // eslint-disable-next-line playwright/no-skipped-test -- The Quick theme tab only mounts when the server has the feature; CI does not enable it by default.
    test.skip(!enabled, "QUICK_THEME feature is not enabled on this server.");
  });

  test("login page preview loads the login theme stylesheet", async ({
    page,
  }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await page.getByTestId("rs-themes-tab").click();
    await page.getByTestId("quickTheme-tab").click();

    const stylesheet = page.locator(
      'link[rel="stylesheet"][href$="/login/keycloak.v2/css/styles.css"]',
    );
    await expect(stylesheet).toHaveCount(1);

    const href = await stylesheet.getAttribute("href");
    const response = await page.request.get(new URL(href!, page.url()).href);
    expect(response.status()).toBe(200);
  });
});
