import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import ProviderPage from "../support/pages/admin-ui/manage/providers/ProviderPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();

const providersPage = new ProviderPage();

const usernameI18nTest = "user_i18n_test";
let usernameI18nId: string;

let originalMasterRealm: RealmRepresentation;

describe("i18n tests", () => {
  before(() => {
    cy.wrap(null).then(async () => {
      const realm = (await adminClient.getRealm("master"))!;
      originalMasterRealm = realm;
      realm.supportedLocales = ["en", "de", "de-CH", "fo"];
      realm.internationalizationEnabled = true;
      await adminClient.updateRealm("master", realm);

      const { id: userId } = await adminClient.createUser({
        username: usernameI18nTest,
        enabled: true,
        credentials: [
          { type: "password", temporary: false, value: usernameI18nTest },
        ],
      });
      usernameI18nId = userId!;

      await adminClient.addRealmRoleToUser(usernameI18nId, "admin");
    });
  });

  after(async () => {
    await adminClient.deleteUser(usernameI18nTest);

    if (originalMasterRealm) {
      await adminClient.updateRealm("master", originalMasterRealm);
    }
  });

  afterEach(async () => {
    await adminClient.removeAllLocalizationTexts();
  });

  const realmLocalizationEn = "realmSettings en";
  const themeLocalizationEn = "Realm settings";
  const realmLocalizationDe = "realmSettings de";
  const themeLocalizationDe = "Realm-Einstellungen";
  const realmLocalizationDeCh = "realmSettings de-CH";

  it("should use THEME localization for fallback (en) when language without theme localization is requested and no realm localization exists", () => {
    updateUserLocale("fo");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(themeLocalizationEn);
  });

  it("should use THEME localization for language when language with theme localization is requested and no realm localization exists", () => {
    updateUserLocale("de");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(themeLocalizationDe);
  });

  it("should use REALM localization for fallback (en) when language without theme localization is requested and realm localization exists for fallback (en)", () => {
    addCommonRealmSettingsLocalizationText("en", realmLocalizationEn);
    updateUserLocale("fo");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(realmLocalizationEn);
  });

  it("should use THEME localization for language when language with theme localization is requested and realm localization exists for fallback (en) only", () => {
    addCommonRealmSettingsLocalizationText("en", realmLocalizationEn);
    updateUserLocale("de");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(themeLocalizationDe);
  });

  it("should use REALM localization for language when language is requested and realm localization exists for language", () => {
    addCommonRealmSettingsLocalizationText("de", realmLocalizationDe);
    updateUserLocale("de");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(realmLocalizationDe);
  });

  // TODO: currently skipped due to https://github.com/keycloak/keycloak/issues/20412
  it.skip("should use REALM localization for region when region is requested and realm localization exists for region", () => {
    addCommonRealmSettingsLocalizationText("de-CH", realmLocalizationDeCh);
    updateUserLocale("de-CH");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(realmLocalizationDeCh);
  });

  it("should use REALM localization for language when language is requested and realm localization exists for fallback (en), language, region", () => {
    addCommonRealmSettingsLocalizationText("en", realmLocalizationEn);
    addCommonRealmSettingsLocalizationText("de", realmLocalizationDe);
    addCommonRealmSettingsLocalizationText("de-CH", realmLocalizationDeCh);
    updateUserLocale("de");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(realmLocalizationDe);
  });

  it("should use REALM localization for language when region is requested and realm localization exists for fallback (en), language", () => {
    addCommonRealmSettingsLocalizationText("en", realmLocalizationEn);
    addCommonRealmSettingsLocalizationText("de", realmLocalizationDe);
    updateUserLocale("de-CH");

    goToUserFederationPage();

    sidebarPage.checkRealmSettingsLinkContainsText(realmLocalizationDe);
  });

  it("should apply plurals and interpolation for THEME localization", () => {
    updateUserLocale("en");

    goToUserFederationPage();

    providersPage.assertCardContainsText("ldap", "Add Ldap providers");
  });

  it("should apply plurals and interpolation for REALM localization", () => {
    addLocalization(
      "en",
      "addProvider_other",
      "addProvider_other en: {{provider}}",
    );
    updateUserLocale("en");

    goToUserFederationPage();

    providersPage.assertCardContainsText("ldap", "addProvider_other en: Ldap");
  });

  function goToUserFederationPage() {
    loginPage.logIn(usernameI18nTest, usernameI18nTest);
    keycloakBefore();
    sidebarPage.goToUserFederation();
  }

  function updateUserLocale(locale: string) {
    cy.wrap(null).then(() =>
      adminClient.updateUser(usernameI18nId, {
        attributes: { locale: locale },
      }),
    );
  }

  function addCommonRealmSettingsLocalizationText(
    locale: string,
    value: string,
  ) {
    addLocalization(locale, "realmSettings", value);
  }

  function addLocalization(locale: string, key: string, value: string) {
    cy.wrap(null).then(() =>
      adminClient.addLocalizationText(locale, key, value),
    );
  }
});
