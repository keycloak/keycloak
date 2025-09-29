import { type Page, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { goToUserFederation } from "../utils/sidebar.ts";
import { assertProviderCardText, assertRealmSettingsText } from "./i18n.ts";

// Test configuration
const testConfig = {
  userId: "",
  username: "user_i18n_test",
  password: "user_i18n_test",
  supportedLocales: ["en", "de", "de-CH", "fo"],
  realmName: `realm-i18n-${uuid()}`,
};

async function setupRealm() {
  const realmName = testConfig.realmName;
  await adminClient.createRealm(realmName, {
    supportedLocales: ["en", "de", "de-CH", "fo"],
    internationalizationEnabled: true,
    enabled: true,
  });
}

async function createUser() {
  const username = testConfig.username;
  const { id } = await adminClient.createUser({
    realm: testConfig.realmName,
    username,
    enabled: true,
    credentials: [{ type: "password", temporary: false, value: username }],
    email: "user_i18n_test@example.com",
    firstName: "User",
    lastName: "I18n",
  });
  testConfig.userId = id!;

  await adminClient.addClientRoleToUser(
    id!,
    "realm-management",
    ["realm-admin"],
    testConfig.realmName,
  );
}

async function updateUserLocale(locale: string) {
  await adminClient.updateUser(testConfig.userId, {
    attributes: { locale: locale },
    realm: testConfig.realmName,
  });
}

async function goToPage(page: Page, locale: string) {
  await updateUserLocale(locale);
  await login(page, {
    realm: testConfig.realmName,
    username: testConfig.username,
    password: testConfig.password,
  });
  await goToUserFederation(page);
}

async function addLocalization(locale: string, key: string, value: string) {
  return adminClient.addLocalizationText(
    locale,
    key,
    value,
    testConfig.realmName,
  );
}

test.describe.serial("i18n tests", () => {
  // Constants for test assertions
  const texts = {
    realmLocalizationEn: "realmSettings en",
    themeLocalizationEn: "Realm settings",
    realmLocalizationDe: "realmSettings de",
    themeLocalizationDe: "Realm-Einstellungen",
    realmLocalizationDeCh: "realmSettings de-CH",
  };

  test.beforeAll(async () => {
    await setupRealm();
    await createUser();
  });

  test.afterAll(() => adminClient.deleteRealm(testConfig.realmName));

  test.afterEach(async () => {
    await adminClient.removeAllLocalizationTexts();
  });

  test("should use THEME localization for fallback when no realm localization exists", async ({
    page,
  }) => {
    await goToPage(page, "fo");
    await assertRealmSettingsText(page, texts.themeLocalizationEn);
  });

  test("should use THEME localization for language with existing theme localization", async ({
    page,
  }) => {
    await goToPage(page, "de");
    await assertRealmSettingsText(page, texts.themeLocalizationDe);
  });

  test("should use REALM localization for language when available", async ({
    page,
  }) => {
    await addLocalization("de", "realmSettings", texts.realmLocalizationDe);
    await goToPage(page, "de");
    await assertRealmSettingsText(page, texts.realmLocalizationDe);
  });

  test("should apply plurals and interpolation for THEME localization", async ({
    page,
  }) => {
    await goToPage(page, "en");
    await assertProviderCardText(page, "ldap", "Add Ldap providers");
  });

  test("should apply plurals and interpolation for REALM localization", async ({
    page,
  }) => {
    await addLocalization(
      "en",
      "addProvider_other",
      "addProvider_other en: {{provider}}",
    );
    await goToPage(page, "en");
    await assertProviderCardText(page, "ldap", "addProvider_other en: Ldap");
  });
});
