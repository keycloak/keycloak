import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toKeysTab } from "../../src/realm-settings/routes/KeysTab.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { switchToggle } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  getRowByCellText,
  searchItem,
} from "../utils/table.ts";
import {
  assertPriority,
  goToAddProviders,
  goToDetails,
  goToKeys,
  switchToFilter,
} from "./keys.ts";

test("Reorders key providers again while the first refresh is pending", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  const { realm } = testBed;
  await login(page, { to: toKeysTab({ realm, tab: "providers" }) });

  const names = page.getByTestId("provider-name-link");
  await expect(names.nth(2)).toBeVisible();
  const [first, second, third, ...remaining] = await names.allTextContents();
  const expectedOrder = [third, second, first, ...remaining];
  const refreshStarted = Promise.withResolvers<boolean>();
  const releaseRefresh = Promise.withResolvers<void>();
  const providersUrl = `**/admin/realms/${realm}/components/?type=org.keycloak.keys.KeyProvider`;
  await page.route(
    providersUrl,
    async (route) => {
      const response = await route.fetch();
      refreshStarted.resolve(response.ok());
      await releaseRefresh.promise;
      await route.fulfill({ response });
    },
    { times: 1 },
  );

  try {
    await getRowByCellText(page, first).dragTo(getRowByCellText(page, third));
    expect(await refreshStarted.promise).toBe(true);
    await expect(names).toHaveText([second, third, first, ...remaining]);

    const secondRefresh = page.waitForResponse(providersUrl);
    await getRowByCellText(page, third).dragTo(getRowByCellText(page, second));
    expect((await secondRefresh).ok()).toBe(true);
    await expect(names).toHaveText(expectedOrder);
  } finally {
    releaseRefresh.resolve();
  }

  await page.reload();
  await expect(names).toHaveText(expectedOrder);
});

test.describe.serial("Realm Settings - Keys", () => {
  const realmName = `events-realm-settings-${uuid()}`;
  const searchPlaceholder = "Search key";

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToKeys(page);
  });

  test("Add Providers", async ({ page }) => {
    await goToAddProviders(page);
    const providers = [
      { option: "option-aes-generated", name: "test_aes-generated" },
      {
        option: "option-ecdsa-generated",
        name: "test_ecdsa-generated",
        switches: ["active", "ecGenerateCertificate"],
      },
      { option: "option-rsa-generated", name: "test_rsa-generated" },
      { option: "option-rsa-enc-generated", name: "test_rsa-enc-generated" },
    ];

    for (const provider of providers) {
      await page.getByTestId("addProviderDropdown").click();
      await page.getByTestId(provider.option).click();
      await page.getByTestId("name").fill(provider.name);
      if (provider.switches) {
        for (const switchId of provider.switches) {
          await switchToggle(page, `#${switchId}`);
        }
      }
      await page.getByTestId("add-provider-button").click();
    }
  });

  test("Search providers", async ({ page }) => {
    await goToAddProviders(page);
    await searchItem(page, "Search", "hmac");
    await assertRowExists(page, "hmac-generated");
  });

  test("Go to details", async ({ page }) => {
    await goToAddProviders(page);
    await goToDetails(page, "hmac-generated");
    await assertPriority(page, "100");
  });

  test("Should search active keys", async ({ page }) => {
    await switchToFilter(page);
    await searchItem(page, searchPlaceholder, "hmac");
    await assertRowExists(page, "hmac-generated-hs512");
  });

  test("Should search passive keys", async ({ page }) => {
    const name = "some-key";
    await adminClient.addKeyProvider(
      name,
      false,
      true,
      "ecdh-generated",
      realmName,
    );
    await goToRealmSettings(page);
    await goToKeys(page);
    await switchToFilter(page, "Passive keys");
    await searchItem(page, searchPlaceholder, "ENC");
    await assertRowExists(page, name);
  });

  test("Should search disabled keys", async ({ page }) => {
    const name = "disabled-key";
    await adminClient.addKeyProvider(
      name,
      true,
      false,
      "ecdh-generated",
      realmName,
    );
    await goToRealmSettings(page);
    await goToKeys(page);
    await switchToFilter(page, "Disabled keys");
    await searchItem(page, searchPlaceholder, "dis");
    await assertRowExists(page, name);
  });

  test("Delete provider", async ({ page }) => {
    await goToAddProviders(page);
    await clickRowKebabItem(page, "hmac-generated-hs512", "Delete");
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "Success. The provider has been deleted.",
    );
  });
});
