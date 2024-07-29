import { v4 as uuid } from "uuid";
import Select from "../../../../forms/Select";

import CommonPage from "../../../CommonPage";
import ListingPage from "../../ListingPage";
import RealmSettingsEventsTab from "./tabs/RealmSettingsEventsTab";

enum RealmSettingsTab {
  Events = "Events",
}

const expect = chai.expect;
export default class RealmSettingsPage extends CommonPage {
  generalSaveBtn = "general-tab-save";
  generalRevertBtn = "general-tab-revert";
  themesSaveBtn = "themes-tab-save";
  sessionsSaveBtn = "sessions-tab-save";
  loginTab = "rs-login-tab";
  emailTab = "rs-email-tab";
  themesTab = "rs-themes-tab";
  localizationTab = "rs-localization-tab";
  securityDefensesTab = "rs-security-defenses-tab";
  sessionsTab = "rs-sessions-tab";
  userProfileTab = "rs-user-profile-tab";
  tokensTab = "rs-tokens-tab";
  selectLoginTheme = "#kc-login-theme";
  loginThemeList = "[data-testid='select-login-theme']";
  selectAccountTheme = "#kc-account-theme";
  accountThemeList = "[data-testid='select-account-theme']";
  selectAdminTheme = "#kc-admin-ui-theme";
  adminThemeList = "[data-testid='select-admin-theme']";
  selectEmailTheme = "#kc-email-theme";
  emailThemeList = "[data-testid='select-email-theme']";
  ssoSessionIdleSelectMenu = "#kc-sso-session-idle-select-menu";
  ssoSessionIdleSelectMenuList = "#kc-sso-session-idle-select-menu ul";
  ssoSessionMaxSelectMenu = "#kc-sso-session-max-select-menu";
  ssoSessionMaxSelectMenuList = "#kc-sso-session-max-select-menu ul";

  ssoSessionMaxRememberMeSelectMenu =
    "#kc-sso-session-max-remember-me-select-menu";
  ssoSessionMaxRememberMeSelectMenuList =
    "#kc-sso-session-max-remember-me-select-menu ul";

  ssoSessionIdleRememberMeSelectMenu =
    "#kc-sso-session-idle-remember-me-select-menu";
  ssoSessionIdleRememberMeSelectMenuList =
    "#kc-sso-session-idle-remember-me-select-menu ul";

  clientSessionIdleSelectMenu = "#kc-client-session-idle-select-menu";
  clientSessionIdleSelectMenuList = "#kc-client-session-idle-select-menu ul";

  clientSessionMaxSelectMenu = "#kc-client-session-max-select-menu";
  clientSessionMaxSelectMenuList = "#kc-client-session-max-select-menu ul";

  offlineSessionIdleSelectMenu = "#kc-offline-session-idle-select-menu";

  loginTimeoutSelectMenu = "#kc-login-timeout-select-menu";
  loginTimeoutSelectMenuList = "#kc-login-timeout-select-menu ul";

  loginActionTimeoutSelectMenu = "#kc-login-action-timeout-select-menu";
  loginActionTimeoutSelectMenuList = "#kc-login-action-timeout-select-menu ul";

  selectDefaultLocale = "#kc-default-locale";
  defaultLocaleList = "select-default-locale";
  supportedLocalesTypeahead = "#supportedLocales";
  supportedLocalesToggle = "#kc-l-supported-locales";
  emailSaveBtn = "email-tab-save";
  managedAccessSwitch = "userManagedAccessAllowed";
  userRegSwitch = "user-reg-switch";
  forgotPwdSwitch = "forgot-pw-switch";
  rememberMeSwitch = "remember-me-switch";
  emailAsUsernameSwitch = "email-as-username-switch";
  editUsernameSwitch = "edit-username-switch";
  loginWithEmailSwitch = "login-with-email-switch";
  duplicateEmailsSwitch = "duplicate-emails-switch";
  verifyEmailSwitch = "verify-email-switch";
  authSwitch = "email-authentication-switch";
  enableSslCheck = "enable-ssl";
  enableStartTlsCheck = "enable-start-tls";
  addProviderDropdown = "addProviderDropdown";
  activeSwitch = "active";
  enabledSwitch = "enabled";
  addProviderButton = "add-provider-button";
  displayName = "name";
  enableEvents = "eventsEnabled";
  eventsUserSave = "save-user";
  enableAdminEvents = "adminEventsEnabled";
  eventsAdminSave = "save-admin";
  eventTypeColumn = "tbody > tr td";
  filterSelectMenu = ".kc-filter-type-select";
  passiveKeysOption = "PASSIVE-option";
  disabledKeysOption = "DISABLED-option";
  activeKeysOption = "ACTIVE-option";
  testConnectionButton = "test-connection-button";
  modalTestConnectionButton = "modal-test-connection-button";
  emailAddressInput = "email-address-input";
  addBundleButton = "add-translationBtn";
  confirmAddTranslation = "add-translation-confirm-button";
  keyInput = "key";
  valueInput = "value";
  deleteAction = "delete-action";
  modalConfirm = "confirm";
  ssoSessionIdleInput = "sso-session-idle-input";
  ssoSessionMaxInput = "sso-session-max-input";
  ssoSessionIdleRememberMeInput = "sso-session-idle-remember-me-input";
  ssoSessionMaxRememberMeInput = "sso-session-max-remember-me-input";
  clientSessionIdleInput = "client-session-idle-input";
  clientSessionMaxInput = "client-session-max-input";
  offlineSessionIdleInput = "offline-session-idle-input";
  offlineSessionMaxSwitch = "offline-session-max-switch";
  loginTimeoutInput = "login-timeout-input";
  loginActionTimeoutInput = "login-action-timeout-input";
  selectDefaultSignatureAlgorithm = "#kc-default-sig-alg";
  revokeRefreshTokenSwitch = "revoke-refresh-token-switch";
  accessTokenLifespanInput = "access-token-lifespan-input";
  accessTokenLifespanImplicitInput = "access-token-lifespan-implicit-input";
  parRequestUriLifespanInput = "par-request-uri-lifespan-input";
  clientLoginTimeoutInput = "client-login-timeout-input";
  offlineSessionMaxInput = "offline-session-max-input";
  userInitiatedActionLifespanInput = "user-initiated-action-lifespan";
  defaultAdminInitatedInput = "default-admin-initated-input";
  emailVerificationInput = "email-verification-input";
  idpEmailVerificationInput = "idp-email-verification-input";
  forgotPasswordInput = "forgot-pw-input";
  executeActionsInput = "execute-actions-input";

  accessTokenLifespanSelectMenu = "#kc-access-token-lifespan-select-menu";
  accessTokenLifespanSelectMenuList =
    "#kc-access-token-lifespan-select-menu ul";

  parRequestUriLifespanSelectMenu = "#par-request-uri-lifespan-select-menu";
  parRequestUriLifespanSelectMenuList =
    "#par-request-uri-lifespan-select-menu ul";

  accessTokenLifespanImplicitSelectMenu =
    "#kc-access-token-lifespan-implicit-select-menu";
  accessTokenLifespanImplicitSelectMenuList =
    "#kc-access-token-lifespan-implicit-select-menu ul";

  clientLoginTimeoutSelectMenu = "#kc-client-login-timeout-select-menu";
  clientLoginTimeoutSelectMenuList = "#kc-client-login-timeout-select-menu ul";

  offlineSessionMaxSelectMenu = "#kc-offline-session-max-select-menu";
  offlineSessionMaxSelectMenuList = "#kc-offline-session-max-select-menu ul";

  userInitiatedActionLifespanSelectMenu =
    "#kc-user-initiated-action-lifespan-select-menu";
  userInitiatedActionLifespanSelectMenuList =
    "#kc-user-initiated-action-lifespan-select-menu ul";

  defaultAdminInitatedInputSelectMenu =
    "#kc-default-admin-initiated-select-menu";
  defaultAdminInitatedInputSelectMenuList =
    "#kc-default-admin-initiated-select-menu";

  emailVerificationSelectMenu = "#kc-email-verification-select-menu";
  emailVerificationSelectMenuList = "#kc-email-verification-select-menu ul";

  idpEmailVerificationSelectMenu = "#kc-idp-email-verification-select-menu";
  idpEmailVerificationSelectMenuList =
    "#kc-idp-email-verification-select-menu ul";

  forgotPasswordSelectMenu = "#kc-forgot-pw-select-menu";
  forgotPasswordSelectMenuList = "#kc-forgot-pw-select-menu ul";

  executeActionsSelectMenu = "#kc-execute-actions-select-menu";
  executeActionsSelectMenuList = "#kc-execute-actions-select-menu ul";

  #formViewProfilesView = "formView-profilesView";
  #jsonEditorProfilesView = "jsonEditor-profilesView";
  #createProfileBtn = "createProfile";
  #formViewSelect = "formView-profilesView";
  #jsonEditorSelect = "jsonEditor-profilesView";
  #formViewSelectPolicies = "formView-policiesView";
  #jsonEditorSelectPolicies = "jsonEditor-policiesView";
  #newClientProfileNameInput = "name";
  #newClientProfileDescriptionInput = "description";
  #saveNewClientProfileBtn = "saveCreateProfile";
  #cancelNewClientProfile = "cancelCreateProfile";
  #createPolicyEmptyStateBtn = "no-client-policies-empty-action";
  #createPolicyBtn = "createPolicy";
  #newClientPolicyNameInput = "name";
  #newClientPolicyDescriptionInput = "client-policy-description";
  #saveNewClientPolicyBtn = "saveCreatePolicy";
  #cancelNewClientPolicyBtn = "cancelCreatePolicy";
  #alertMessage = ".pf-v5-c-alert__title";
  #modalDialogTitle = ".pf-v5-c-modal-box__title-text";
  #modalDialogBodyText = ".pf-v5-c-modal-box__body";
  #deleteDialogCancelBtn = "#modal-cancel";
  #jsonEditorSaveBtn = "jsonEditor-saveBtn";
  #jsonEditorSavePoliciesBtn = "jsonEditor-policies-saveBtn";
  #jsonEditorReloadBtn = "jsonEditor-reloadBtn";
  #jsonEditor = ".monaco-scrollable-element.editor-scrollable.vs";
  #clientPolicyDrpDwn = '[data-testid="action-dropdown"]';
  #deleteclientPolicyDrpDwn = "deleteClientPolicyDropdown";
  #clientProfileOne =
    'a[href*="realm-settings/client-policies/Test/edit-profile"]';
  #clientProfileTwo =
    'a[href*="realm-settings/client-policies/Edit/edit-profile"]';
  #clientPolicy = 'a[href*="realm-settings/client-policies/Test/edit-policy"]';
  #reloadBtn = "reloadProfile";
  #addExecutor = "addExecutor";
  #addExecutorDrpDwn = "#kc-executor";
  #addExecutorDrpDwnOption = ".pf-v5-c-menu__list";
  #addExecutorCancelBtn = ".pf-v5-c-form__actions a";
  #addExecutorSaveBtn = "addExecutor-saveBtn";
  #availablePeriodExecutorFld = "available-period";
  #editExecutorBtn =
    '[aria-label="Executors"] > li > div:first-child [data-testid="editExecutor"]';
  #executorAvailablePeriodInput = "[data-testid='available-period']";

  #listingPage = new ListingPage();
  #addCondition = "addCondition";
  #addConditionDrpDwn = "#provider";
  #addConditionDrpDwnOption = ".pf-v5-c-menu__list";
  #addConditionCancelBtn = "addCondition-cancelBtn";
  #addConditionSaveBtn = "addCondition-saveBtn";
  #clientRolesConditionLink = "client-roles-condition-link";
  #clientScopesConditionLink = "client-scopes-condition-link";
  #eventListenersFormLabel = ".pf-v5-c-form__label-text";
  #eventListenersDrpDwn = "#eventsListeners";
  #eventListenersSaveBtn = "saveEventListenerBtn";
  #eventListenersRevertBtn = "revertEventListenerBtn";
  #eventListenersInputFld =
    "#eventsListeners .pf-v5-c-text-input-group__text-input";
  #eventListenersDrpDwnOption = ".pf-v5-c-menu__list";
  #eventListenersDrwDwnSelect = "#eventsListeners .pf-v5-c-menu-toggle__button";
  #eventListenerRemove = ".pf-v5-c-chip__actions";
  #roleSelect = "config.roles0";
  #selectScopeButton = "addValue";
  #deleteClientRolesConditionBtn = "delete-client-roles-condition";
  #deleteClientScopesConditionBtn = "delete-client-scopes-condition";
  #fromDisplayName = "smtpServer.fromDisplayName";
  #replyToEmail = "smtpServer.replyTo";
  #port = "smtpServer.port";

  #publicKeyBtn = ".kc-keys-list > tbody > tr > td > .button-wrapper > button";
  #localizationLocalesSubTab = "rs-localization-locales-tab";
  #localizationRealmOverridesSubTab = "rs-localization-realm-overrides-tab";
  #localizationEffectiveMessageBundlesSubTab =
    "rs-localization-effective-message-bundles-tab";
  #realmSettingsEventsTab = new RealmSettingsEventsTab();
  #realmId = 'input[aria-label="Copyable input"]';
  #securityDefensesHeadersSaveBtn = "headers-form-tab-save";
  #securityDefensesBruteForceSaveBtn = "brute-force-tab-save";
  #securityDefensesHeadersTab = "security-defenses-headers-tab";
  #securityDefensesBruteForceTab = "security-defenses-brute-force-tab";
  #clientProfileLink = 'table[aria-label="Profiles"] tbody a';

  #realmName?: string;
  constructor(realmName?: string) {
    super();
    this.#realmName = realmName;
  }

  #getRealmDisplayName() {
    return cy.findByTestId("displayName");
  }

  #getFrontEndURL() {
    return cy.findByTestId("attributes.frontendUrl");
  }

  #getSSLRequired() {
    return cy.get("#sslRequired");
  }

  #getUnmanagedAttributes() {
    return cy.get("#unmanagedAttributePolicy");
  }

  getFromInput() {
    return cy.findByTestId("smtpServer.from");
  }

  getHostInput() {
    return cy.findByTestId("smtpServer.host");
  }

  goToEventsTab() {
    this.tabUtils().clickTab(RealmSettingsTab.Events);
    return this.#realmSettingsEventsTab;
  }

  disableRealm() {
    cy.get(this.#modalDialogTitle).contains("Disable realm?");
    cy.get(this.#modalDialogBodyText).contains(
      "User and clients can't access the realm if it's disabled. Are you sure you want to continue?",
    );
    cy.findByTestId(this.modalConfirm).click();
  }
  selectLoginThemeType(themeType: string) {
    cy.get(this.selectLoginTheme).click();
    cy.get(this.loginThemeList).contains(themeType).click();

    return this;
  }

  selectAccountThemeType(themeType: string) {
    cy.get(this.selectAccountTheme).click();
    cy.get(this.accountThemeList).contains(themeType).click();
    return this;
  }

  selectAdminThemeType(themeType: string) {
    cy.get(this.selectAdminTheme).click();
    cy.get(this.adminThemeList).contains(themeType).click();
    return this;
  }

  selectEmailThemeType(themeType: string) {
    cy.get(this.selectEmailTheme).click();
    cy.get(this.emailThemeList).contains(themeType).click();
    return this;
  }

  fillEmailField(email: string) {
    cy.findByTestId(this.emailAddressInput).type(email);
    return this;
  }

  fillHostField(host: string) {
    this.getHostInput().clear();
    this.getHostInput().type(host);
    return this;
  }

  getDisplayName(name: string) {
    this.#getRealmDisplayName().should("have.value", name);
    return this;
  }

  getFrontendURL(url: string) {
    this.#getFrontEndURL().should("have.value", url);
    return this;
  }

  getRequireSSL(option: string) {
    Select.assertSelectedItem(this.#getSSLRequired(), option);
    return this;
  }

  getUnmanagedAttributes(option: string) {
    Select.assertSelectedItem(this.#getUnmanagedAttributes(), option);
    return this;
  }

  fillDisplayName(displayName: string) {
    this.#getRealmDisplayName().clear();
    this.#getRealmDisplayName().type(displayName);
  }

  clearRealmId() {
    cy.get(this.#realmId).clear();
  }

  fillFromDisplayName(displayName: string) {
    cy.findByTestId(this.#fromDisplayName).clear().type(displayName);
  }

  fillReplyToEmail(email: string) {
    cy.findByTestId(this.#replyToEmail).clear();
    cy.findByTestId(this.#replyToEmail).type(email);
  }

  fillPort(port: string) {
    cy.findByTestId(this.#port).clear();
    cy.findByTestId(this.#port).type(port);
  }

  fillFrontendURL(url: string) {
    this.clearFrontendURL();
    this.#getFrontEndURL().type(url);
  }

  clearFrontendURL() {
    this.#getFrontEndURL().clear();
  }

  fillRequireSSL(option: string) {
    Select.selectItem(this.#getSSLRequired(), option);
  }

  fillUnmanagedAttributes(option: string) {
    Select.selectItem(this.#getUnmanagedAttributes(), option);
  }

  setDefaultLocale(locale: string) {
    cy.get(this.selectDefaultLocale).click();
    cy.findByTestId(this.defaultLocaleList).contains(locale).click();
    return this;
  }

  saveGeneral() {
    cy.findByTestId(this.generalSaveBtn).click();

    return this;
  }

  saveThemes() {
    cy.findByTestId(this.themesSaveBtn).click();

    return this;
  }

  saveSessions() {
    cy.findByTestId(this.sessionsSaveBtn).click();

    return this;
  }

  addSenderEmail(senderEmail: string) {
    this.getFromInput().clear();

    if (senderEmail) {
      this.getFromInput().type(senderEmail);
    }

    return this;
  }

  testSelectFilter() {
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.passiveKeysOption).click();
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.disabledKeysOption).click();
  }

  deleteProvider(name: string) {
    this.#listingPage.deleteItem(name);
    this.modalUtils().checkModalTitle("Delete key provider?").confirmModal();

    cy.get(this.#alertMessage).should(
      "be.visible",
      "Success. The provider has been deleted.",
    );
    return this;
  }

  checkKeyPublic() {
    cy.get(this.#publicKeyBtn).contains("Public key").click();
    this.modalUtils().checkModalTitle("Public key").confirmModal();

    cy.get(this.#publicKeyBtn).contains("Certificate").click();
    this.modalUtils().checkModalTitle("Certificate").confirmModal();
  }

  switchToActiveFilter() {
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.activeKeysOption).click();
  }

  switchToPassiveFilter() {
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.passiveKeysOption).click();
  }

  switchToDisabledFilter() {
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.disabledKeysOption).click();
  }

  toggleSwitch(switchName: string, waitFor: boolean | undefined = true) {
    const loadName = `load-${uuid()}`;
    if (waitFor) {
      cy.intercept({ path: "/admin/realms/*", times: 1 }).as(loadName);
    }
    cy.findByTestId(switchName).click({ force: true });
    if (waitFor) {
      cy.wait(`@${loadName}`);
    }

    return this;
  }

  assertSwitch(switchName: string, on: boolean) {
    cy.findByTestId(switchName).should("have.value", on ? "on" : "off");

    return this;
  }

  setSwitch(switchName: string, on: boolean) {
    if (on) {
      cy.findByTestId(switchName).check({ force: true });
    } else {
      cy.findByTestId(switchName).uncheck({ force: true });
    }

    return this;
  }

  toggleCheck(switchName: string) {
    cy.findByTestId(switchName).click();

    return this;
  }

  toggleAddProviderDropdown() {
    const keysUrl = `/admin/realms/${this.#realmName}/keys`;
    cy.intercept(keysUrl).as("keysFetch");
    cy.findByTestId(this.addProviderDropdown).click();

    return this;
  }

  addProvider() {
    cy.findByTestId(this.addProviderButton).click();

    return this;
  }

  addKeyValuePair(key: string, value: string) {
    cy.findByTestId(this.addBundleButton).click();

    cy.findByTestId(this.keyInput).type(key);
    cy.findByTestId(this.valueInput).type(value);

    cy.findByTestId(this.confirmAddTranslation).click({ force: true });

    return this;
  }

  enterUIDisplayName(name: string) {
    cy.findByTestId(this.displayName).clear().type(name);
  }

  save(saveBtn: string) {
    cy.findByTestId(saveBtn).click();

    return this;
  }

  revert(revertBtn: string) {
    cy.findByTestId(revertBtn).click();

    return this;
  }

  clearEvents(type: "admin" | "user") {
    cy.findByTestId(`clear-${type}-events`).click();

    return this;
  }

  addUserEvents(events: string[]) {
    cy.findByTestId("addTypes").click();
    for (const event of events) {
      cy.get(this.eventTypeColumn)
        .contains(event)
        .parent()
        .find("input")
        .click();
    }
    return this;
  }

  changeTimeUnit(
    unit: "Minutes" | "Hours" | "Days",
    inputType: string,
    listType: string,
  ) {
    switch (unit) {
      case "Minutes":
        cy.get(inputType).click();
        cy.get(listType).contains(unit).click();
        break;
      case "Hours":
        cy.get(inputType).click();
        cy.get(listType).contains(unit).click();
        break;
      case "Days":
        cy.get(inputType).click();
        cy.get(listType).contains(unit).click();
        break;
      default:
        throw "Invalid unit, must be 'minutes', 'hours', or 'days'.";
    }
    return this;
  }

  populateSessionsPage() {
    cy.findByTestId(this.ssoSessionIdleInput).clear().type("1");
    this.changeTimeUnit(
      "Minutes",
      this.ssoSessionIdleSelectMenu,
      this.ssoSessionIdleSelectMenuList,
    );
    cy.findByTestId(this.ssoSessionMaxInput).clear().type("2");
    this.changeTimeUnit(
      "Hours",
      this.ssoSessionMaxSelectMenu,
      this.ssoSessionMaxSelectMenuList,
    );
    cy.findByTestId(this.ssoSessionIdleRememberMeInput).clear().type("3");
    this.changeTimeUnit(
      "Days",
      this.ssoSessionIdleRememberMeSelectMenu,
      this.ssoSessionIdleRememberMeSelectMenuList,
    );
    cy.findByTestId(this.ssoSessionMaxRememberMeInput).clear().type("4");
    this.changeTimeUnit(
      "Minutes",
      this.ssoSessionMaxRememberMeSelectMenu,
      this.ssoSessionMaxRememberMeSelectMenuList,
    );

    cy.findByTestId(this.clientSessionIdleInput).clear().type("5");
    this.changeTimeUnit(
      "Hours",
      this.clientSessionIdleSelectMenu,
      this.clientSessionIdleSelectMenuList,
    );
    cy.findByTestId(this.clientSessionMaxInput).clear().type("6");
    this.changeTimeUnit(
      "Days",
      this.clientSessionMaxSelectMenu,
      this.clientSessionMaxSelectMenuList,
    );

    cy.findByTestId(this.offlineSessionIdleInput).clear().type("7");
    this.toggleSwitch(this.offlineSessionMaxSwitch, false);

    cy.findByTestId(this.loginTimeoutInput).clear().type("9");
    this.changeTimeUnit(
      "Minutes",
      this.loginTimeoutSelectMenu,
      this.loginTimeoutSelectMenuList,
    );
    cy.findByTestId(this.loginActionTimeoutInput).clear().type("10");
    this.changeTimeUnit(
      "Days",
      this.loginActionTimeoutSelectMenu,
      this.loginActionTimeoutSelectMenuList,
    );
  }

  populateTokensPage() {
    this.toggleSwitch(this.revokeRefreshTokenSwitch, false);

    cy.findByTestId(this.accessTokenLifespanInput)
      .focus()
      .clear({ force: true });
    cy.findByTestId(this.accessTokenLifespanInput).clear().type("1");
    this.changeTimeUnit(
      "Days",
      this.accessTokenLifespanSelectMenu,
      this.accessTokenLifespanSelectMenuList,
    );
    cy.findByTestId(this.accessTokenLifespanImplicitInput).clear().type("2");
    this.changeTimeUnit(
      "Minutes",
      this.accessTokenLifespanImplicitSelectMenu,
      this.accessTokenLifespanImplicitSelectMenuList,
    );
    cy.findByTestId("par-request-uri-lifespan-input").clear().type("2");
    this.changeTimeUnit(
      "Hours",
      this.parRequestUriLifespanSelectMenu,
      this.parRequestUriLifespanSelectMenuList,
    );

    cy.findByTestId(this.clientLoginTimeoutInput).clear().type("3");
    this.changeTimeUnit(
      "Hours",
      this.clientLoginTimeoutSelectMenu,
      this.clientLoginTimeoutSelectMenuList,
    );

    cy.findByTestId(this.userInitiatedActionLifespanInput).clear().type("4");
    this.changeTimeUnit(
      "Minutes",
      this.userInitiatedActionLifespanSelectMenu,
      this.userInitiatedActionLifespanSelectMenuList,
    );

    cy.findByTestId(this.defaultAdminInitatedInput).clear().type("5");
    this.changeTimeUnit(
      "Days",
      this.defaultAdminInitatedInputSelectMenu,
      this.defaultAdminInitatedInputSelectMenuList,
    );

    cy.findByTestId(this.emailVerificationInput).clear().type("6");
    this.changeTimeUnit(
      "Days",
      this.emailVerificationSelectMenu,
      this.emailVerificationSelectMenuList,
    );

    cy.findByTestId(this.idpEmailVerificationInput).clear().type("7");
    this.changeTimeUnit(
      "Days",
      this.idpEmailVerificationSelectMenu,
      this.idpEmailVerificationSelectMenuList,
    );

    cy.findByTestId(this.forgotPasswordInput).clear().type("8");
    this.changeTimeUnit(
      "Days",
      this.forgotPasswordSelectMenu,
      this.forgotPasswordSelectMenuList,
    );
    cy.findByTestId(this.executeActionsInput).clear().type("9");
    this.changeTimeUnit(
      "Days",
      this.executeActionsSelectMenu,
      this.executeActionsSelectMenuList,
    );
  }

  checkUserEvents(events: string[]) {
    cy.get(this.eventTypeColumn).should((event) => {
      for (const user of events) {
        expect(event).to.contain(user);
      }
    });
    return this;
  }

  setOfflineSessionMaxSwitch(value: boolean) {
    this.setSwitch(this.offlineSessionMaxSwitch, value);
    return this;
  }

  clickAdd() {
    cy.findByTestId("addEventTypeConfirm").click();
    return this;
  }

  shouldDisplayEventListenersForm() {
    cy.get(this.#eventListenersFormLabel)
      .should("be.visible")
      .contains("Event listeners");
    cy.get(this.#eventListenersDrpDwn).should("exist");
    cy.findByTestId(this.#eventListenersSaveBtn).should("exist");
    cy.findAllByTestId(this.#eventListenersRevertBtn).should("exist");
  }

  shouldRevertSavingEventListener() {
    cy.get(this.#eventListenersInputFld).click().type("email");
    cy.get(this.#eventListenersDrpDwnOption).click();
    cy.get(this.#eventListenersDrwDwnSelect).click();
    cy.findByTestId(this.#eventListenersRevertBtn).click();
    cy.get(this.#eventListenersDrpDwn).should("not.have.text", "email");
  }

  shouldSaveEventListener() {
    cy.get(this.#eventListenersInputFld).click().type("email");
    cy.get(this.#eventListenersDrpDwnOption).click();
    cy.get(this.#eventListenersDrwDwnSelect).click();
    cy.findByTestId(this.#eventListenersSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Event listener has been updated.",
    );
  }

  shouldRemoveEventFromEventListener() {
    cy.get(this.#eventListenerRemove).last().click({ force: true });
    cy.get(this.#eventListenersInputFld).click();
    cy.findByTestId(this.#eventListenersSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Event listener has been updated.",
    );
    cy.get(this.#eventListenersDrpDwn).should("not.have.text", "email");
  }

  shouldRemoveAllEventListeners() {
    cy.get(".pf-v5-c-chip__actions").first().click();
    cy.get(".pf-v5-c-chip__actions").click();
    cy.findByTestId(this.#eventListenersSaveBtn).click();
    cy.get(this.#eventListenersDrpDwn).should("not.have.text", "jboss-logging");
    cy.get(this.#eventListenersDrpDwn).should("not.have.text", "email");
  }

  shouldReSaveEventListener() {
    cy.get(this.#eventListenersInputFld).click().type("jboss-logging");
    cy.get(this.#eventListenersDrpDwnOption).click();
    cy.get(this.#eventListenersDrwDwnSelect).click();
    cy.findByTestId(this.#eventListenersSaveBtn).click();
  }

  shouldDisplayProfilesTab() {
    cy.findByTestId(this.#createProfileBtn).should("exist");
    cy.findByTestId(this.#formViewSelect).should("exist");
    cy.findByTestId(this.#jsonEditorSelect).should("exist");
    cy.get("table").should("be.visible").contains("td", "Global");
  }

  shouldDisplayNewClientProfileForm() {
    cy.findByTestId(this.#createProfileBtn).click();
    cy.findByTestId(this.#newClientProfileNameInput).should("exist");
    cy.findByTestId(this.#newClientProfileDescriptionInput).should("exist");
    cy.findByTestId(this.#saveNewClientProfileBtn).should("exist");
    cy.findByTestId(this.#cancelNewClientProfile).should("exist");
  }

  createClientProfile(name: string, description: string) {
    cy.findByTestId(this.#createProfileBtn).click();
    cy.findByTestId(this.#newClientProfileNameInput).type(name);
    cy.findByTestId(this.#newClientProfileDescriptionInput).type(description);
    return this;
  }

  saveClientProfileCreation() {
    cy.findByTestId(this.#saveNewClientProfileBtn).click();
    return this;
  }

  cancelClientProfileCreation() {
    cy.findByTestId(this.#cancelNewClientProfile).click();
    return this;
  }

  shouldSearchClientProfile() {
    new ListingPage().searchItem("Test", false).itemExist("Test");
    return this;
  }

  cancelDeleteClientPolicy() {
    cy.get(this.#deleteDialogCancelBtn)
      .contains("Cancel")
      .click({ force: true });
    cy.get("table").should("be.visible").contains("td", "Test");
    return this;
  }

  deleteClientPolicyItemFromTable(name: string) {
    this.#listingPage.searchItem(name, false);
    this.#listingPage.clickRowDetails(name).clickDetailMenu("Delete");
    return this;
  }

  shouldNavigateBetweenFormAndJSONView() {
    cy.findByTestId(this.#jsonEditorProfilesView).check();
    cy.findByTestId(this.#jsonEditorSaveBtn).contains("Save");
    cy.findByTestId(this.#jsonEditorReloadBtn).contains("Reload");
    cy.findByTestId(this.#formViewProfilesView).check();
    cy.findByTestId(this.#createProfileBtn).contains("Create client profile");
  }

  shouldSaveChangedJSONProfiles() {
    cy.findByTestId(this.#jsonEditorProfilesView).check();
    cy.get(this.#jsonEditor).type(`{pageup}{del} [{
      "name": "Test",
      "description": "Test Description",
      "executors": [],
      "global": false
    }, {downarrow}{end}{backspace}{backspace}`);
    cy.findByTestId(this.#jsonEditorSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "The client profiles configuration was updated",
    );
    cy.findByTestId(this.#formViewProfilesView).check();
    cy.get("table").should("be.visible").contains("td", "Test");
  }

  shouldEditClientProfile() {
    cy.get(this.#clientProfileOne).click();
    cy.findByTestId(this.#newClientProfileNameInput)
      .click()
      .clear()
      .type("Edit");
    cy.findByTestId(this.#newClientProfileDescriptionInput)
      .click()
      .clear()
      .type("Edit Description");
    cy.findByTestId(this.#saveNewClientProfileBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Client profile updated successfully",
    );
  }

  shouldCheckEditedClientProfileListed() {
    cy.get("table").should("be.visible").contains("td", "Edit");
    cy.get("table").should("not.have.text", "Test");
  }

  shouldShowErrorWhenNameBlank() {
    cy.get(this.#clientProfileTwo).click();
    cy.findByTestId(this.#newClientProfileNameInput).click().clear();
    cy.get("form").should("not.have.text", "Required field");
  }

  shouldShowErrorWhenDuplicate() {
    cy.get("form").should(
      "not.have.text",
      "The name must be unique within the realm",
    );
  }

  shouldReloadClientProfileEdits() {
    cy.get(this.#clientProfileTwo).click();
    cy.findByTestId(this.#newClientProfileNameInput).type("Reloading");
    cy.findByTestId(this.#reloadBtn).click();
    cy.findByTestId(this.#newClientProfileNameInput).should(
      "have.value",
      "Edit",
    );
  }

  shouldNotHaveExecutorsConfigured() {
    cy.get(this.#clientProfileTwo).click();
    cy.get('h2[class*="kc-emptyExecutors"]').should(
      "have.text",
      "No executors configured",
    );
  }

  shouldCancelAddingExecutor() {
    cy.get(this.#clientProfileTwo).click();
    cy.findByTestId(this.#addExecutor).click();
    cy.get(this.#addExecutorDrpDwn).click();
    cy.get(this.#addExecutorDrpDwnOption)
      .contains("secure-ciba-signed-authn-req")
      .click();
    cy.get(this.#addExecutorCancelBtn).click();
    cy.get('h2[class*="kc-emptyExecutors"]').should(
      "have.text",
      "No executors configured",
    );
  }

  shouldAddExecutor() {
    cy.get(this.#clientProfileTwo).click();
    cy.findByTestId(this.#addExecutor).click();
    cy.get(this.#addExecutorDrpDwn).click();
    cy.get(this.#addExecutorDrpDwnOption)
      .contains("secure-ciba-signed-authn-req")
      .click();
    cy.findByTestId(this.#addExecutorSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Success! Executor created successfully",
    );
    cy.get('ul[class*="pf-v5-c-data-list"]').should(
      "have.text",
      "secure-ciba-signed-authn-req",
    );
  }

  shouldCancelDeletingExecutor() {
    cy.get(this.#clientProfileTwo).click();
    cy.get('svg[class*="kc-executor-trash-icon"]').click();
    cy.get(this.#modalDialogTitle).contains("Delete executor?");
    cy.get(this.#modalDialogBodyText).contains(
      "The action will permanently delete secure-ciba-signed-authn-req. This cannot be undone.",
    );
    cy.findByTestId(this.modalConfirm).contains("Delete");
    cy.get(this.#deleteDialogCancelBtn).contains("Cancel").click();
    cy.get('ul[class*="pf-v5-c-data-list"]').should(
      "have.text",
      "secure-ciba-signed-authn-req",
    );
  }

  openProfileDetails(name: string) {
    cy.intercept(
      `/admin/realms/${this.#realmName}/client-policies/profiles*`,
    ).as("profilesFetch");
    cy.get(
      'a[href*="realm-settings/client-policies/' + name + '/edit-profile"]',
    ).click();
    cy.wait("@profilesFetch");
    return this;
  }

  editExecutor(availablePeriod?: number) {
    cy.intercept(
      `/admin/realms/${this.#realmName}/client-policies/profiles*`,
    ).as("profilesFetch");
    cy.get(this.#editExecutorBtn).click();
    cy.wait("@profilesFetch");
    if (availablePeriod) {
      cy.get(this.#executorAvailablePeriodInput)
        .clear()
        .type(availablePeriod.toString());
    }
    return this;
  }

  saveExecutor() {
    cy.findByTestId(this.#addExecutorSaveBtn).click();
    return this;
  }

  cancelEditingExecutor() {
    cy.get(this.#addExecutorCancelBtn)
      .contains("Cancel")
      .click({ force: true });
    return this;
  }

  checkExecutorNotInList() {
    cy.get('ul[class*="pf-v5-c-data-list"]').should(
      "have.text",
      "secure-ciba-signed-authn-req",
    );
    return this;
  }

  checkAvailablePeriodExecutor(value: number) {
    cy.findByTestId(this.#availablePeriodExecutorFld).should(
      "have.value",
      value,
    );
    return this;
  }

  shouldEditExecutor() {
    cy.get(this.#clientProfileTwo).click();
    cy.get(this.#editExecutorBtn).click();
    cy.findByTestId(this.#availablePeriodExecutorFld).clear().type("4000");
    cy.findByTestId(this.#addExecutorSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Executor updated successfully",
    );
  }

  shouldDeleteExecutor() {
    cy.get(this.#clientProfileTwo).click();
    cy.get('svg[class*="kc-executor-trash-icon"]').click();
    cy.get(this.#modalDialogTitle).contains("Delete executor?");
    cy.get(this.#modalDialogBodyText).contains(
      "The action will permanently delete secure-ciba-signed-authn-req. This cannot be undone.",
    );
    cy.findByTestId(this.modalConfirm).contains("Delete");
    cy.findByTestId(this.modalConfirm).click();
    cy.get('h2[class*="kc-emptyExecutors"]').should(
      "have.text",
      "No executors configured",
    );
  }

  shouldReloadJSONProfiles() {
    cy.findByTestId(this.#jsonEditorProfilesView).check();
    cy.findByTestId(this.#jsonEditorReloadBtn).contains("Reload").click();
    cy.findByTestId(this.#jsonEditorSaveBtn).contains("Save");
    cy.findByTestId(this.#jsonEditorReloadBtn).contains("Reload");
  }

  shouldSaveChangedJSONPolicies() {
    cy.findByTestId(this.#jsonEditorSelectPolicies).check();
    cy.findByTestId(this.#jsonEditorReloadBtn).click();

    cy.get(this.#jsonEditor).type(`{pageup}{del} [{
      "name": "Reload",
    }, {downarrow}{end}{backspace}{backspace}{backspace}{backspace}`);

    cy.findByTestId(this.#jsonEditorReloadBtn).click();

    cy.get(this.#jsonEditor).type(`{pageup}{del} [{
      "name": "Test",
      "description": "Test Description",
      "enabled": false,
      "conditions": [],
      "profiles": [],
    }, {downarrow}{end}{backspace}{backspace}{backspace}{backspace}`);

    cy.findByTestId(this.#jsonEditorSavePoliciesBtn).click();

    cy.get(this.#alertMessage).should(
      "be.visible",
      "The client policy configuration was updated",
    );
    cy.findByTestId(this.#formViewSelectPolicies).check();
    cy.get("table").should("be.visible").contains("td", "Test");
  }

  shouldNavigateBetweenFormAndJSONViewPolicies() {
    cy.findByTestId(this.#jsonEditorSelectPolicies).check();
    cy.findByTestId(this.#jsonEditorSavePoliciesBtn).contains("Save");
    cy.findByTestId(this.#jsonEditorReloadBtn).contains("Reload");
    cy.findByTestId(this.#formViewSelectPolicies).check();
    cy.findByTestId(this.#createPolicyEmptyStateBtn).contains(
      "Create client policy",
    );
  }

  checkDisplayPoliciesTab() {
    cy.findByTestId(this.#createPolicyEmptyStateBtn).should("exist");
    cy.findByTestId(this.#formViewSelectPolicies).should("exist");
    cy.findByTestId(this.#jsonEditorSelectPolicies).should("exist");
    return this;
  }

  checkNewClientPolicyForm() {
    cy.findByTestId(this.#newClientPolicyNameInput).should("exist");
    cy.findByTestId(this.#newClientPolicyDescriptionInput).should("exist");
    cy.findByTestId(this.#saveNewClientPolicyBtn).should("exist");
    cy.findByTestId(this.#cancelNewClientPolicyBtn).should("exist");
    return this;
  }

  cancelNewClientPolicyCreation() {
    cy.findByTestId(this.#cancelNewClientPolicyBtn).click();
    return this;
  }

  createNewClientPolicyFromList(
    name: string,
    description: string,
    cancel?: boolean,
  ) {
    cy.findByTestId(this.#createPolicyBtn).click();
    cy.findByTestId(this.#newClientPolicyNameInput).type(name);
    cy.findByTestId(this.#newClientPolicyDescriptionInput).type(description);
    if (!cancel) {
      cy.findByTestId(this.#saveNewClientPolicyBtn).click();
    }
    return this;
  }

  searchClientPolicy(name: string) {
    new ListingPage().searchItem(name, false).itemExist(name);
    return this;
  }

  searchClientProfile(name: string) {
    new ListingPage().searchItem(name, false).itemExist(name);
    return this;
  }

  searchNonExistingClientProfile(name: string) {
    new ListingPage().searchItem(name, false);
    return this;
  }

  shouldNotHaveConditionsConfigured() {
    cy.get(this.#clientPolicy).click();
    cy.get('h2[class*="kc-emptyConditions"]').should(
      "have.text",
      "No conditions configured",
    );
  }

  shouldCancelAddingCondition() {
    cy.get(this.#clientPolicy).click();
    cy.findByTestId(this.#addCondition).click();
    cy.get(this.#addConditionDrpDwn).click();
    cy.get(this.#addConditionDrpDwnOption).contains("any-client").click();
    cy.findByTestId(this.#addConditionCancelBtn).click();
    cy.get('h2[class*="kc-emptyConditions"]').should(
      "have.text",
      "No conditions configured",
    );
  }

  shouldAddClientRolesCondition() {
    cy.get(this.#clientPolicy).click();
    cy.findByTestId(this.#addCondition).click();
    cy.get(this.#addConditionDrpDwn).click();
    cy.get(this.#addConditionDrpDwnOption).contains("client-roles").click();
    cy.findByTestId(this.#roleSelect).clear().type("manage-realm");

    cy.findByTestId(this.#addConditionSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Success! Condition created successfully",
    );
    cy.get('ul[class*="pf-v5-c-data-list"]').should(
      "have.text",
      "client-roles",
    );
  }

  addClientScopes() {
    cy.findByTestId("config.scopes0").clear().type("one");
    cy.findByTestId(this.#selectScopeButton).click();
    cy.findByTestId("config.scopes1").clear().type("two");
    cy.findByTestId(this.#selectScopeButton).click();
    cy.findByTestId("config.scopes2").clear().type("three");
  }

  shouldAddClientScopesCondition() {
    cy.get(this.#clientPolicy).click();
    cy.findByTestId(this.#addCondition).click();
    cy.get(this.#addConditionDrpDwn).click();
    cy.get(this.#addConditionDrpDwnOption).contains("client-scopes").click();

    this.addClientScopes();

    cy.findByTestId(this.#addConditionSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Success! Condition created successfully",
    );
    cy.get('ul[class*="pf-v5-c-data-list"]').contains("client-scopes");
  }

  shouldEditClientRolesCondition() {
    cy.get(this.#clientPolicy).click();

    cy.findByTestId(this.#clientRolesConditionLink).click();

    cy.findByTestId(this.#roleSelect).should("have.value", "manage-realm");
    cy.findByTestId(this.#roleSelect).clear().type("admin");

    cy.findByTestId(this.#addConditionSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Success! Condition updated successfully",
    );
  }

  shouldEditClientScopesCondition() {
    cy.get(this.#clientPolicy).click();

    cy.findByTestId(this.#clientScopesConditionLink).click();

    cy.findByTestId("config.scopes0").clear().type("edit");

    cy.findByTestId(this.#addConditionSaveBtn).click();
    cy.get(this.#alertMessage).should(
      "be.visible",
      "Success! Condition updated successfully",
    );
  }

  checkConditionsListContains(name: string) {
    cy.get('ul[class*="pf-v5-c-data-list"]').contains(name);
    return this;
  }

  deleteClientRolesCondition() {
    cy.get(this.#clientPolicy).click();
    cy.findByTestId(this.#deleteClientRolesConditionBtn).click();
    return this;
  }

  shouldDeleteClientScopesCondition() {
    cy.get(this.#clientPolicy).click();
    cy.findByTestId(this.#deleteClientScopesConditionBtn).click();
    cy.get(this.#modalDialogTitle).contains("Delete condition?");
    cy.get(this.#modalDialogBodyText).contains(
      "This action will permanently delete client-scopes. This cannot be undone.",
    );
    cy.findByTestId(this.modalConfirm).contains("Delete");
    cy.findByTestId(this.modalConfirm).click({ force: true });
    cy.get('h2[class*="kc-emptyConditions"]').should(
      "have.text",
      "No conditions configured",
    );
  }

  goToClientPoliciesTab() {
    cy.findByTestId("rs-clientPolicies-tab").click();
    return this;
  }

  goToClientPoliciesList() {
    cy.findByTestId("rs-policies-clientPolicies-tab").click();
    return this;
  }

  goToClientProfilesList() {
    cy.findByTestId("rs-policies-clientProfiles-tab").click();
    return this;
  }

  createNewClientPolicyFromEmptyState(
    name: string,
    description: string,
    cancel?: boolean,
  ) {
    cy.findByTestId(this.#createPolicyEmptyStateBtn).click();
    cy.findByTestId(this.#newClientPolicyNameInput).type(name);
    cy.findByTestId(this.#newClientPolicyDescriptionInput).type(description);
    if (!cancel) {
      cy.findByTestId(this.#saveNewClientPolicyBtn).click();
    }
    return this;
  }

  checkEmptyPolicyList() {
    cy.findByTestId(this.#createPolicyEmptyStateBtn).should("exist");
    return this;
  }

  checkElementNotInList(name: string) {
    cy.get("tbody").should("not.contain.text", name);
    return this;
  }

  checkElementInList(name: string) {
    cy.get("tbody").should("contain.text", name);
    return this;
  }

  deleteClientPolicyFromDetails() {
    cy.get(this.#clientPolicyDrpDwn).click({ force: true });
    cy.findByTestId(this.#deleteclientPolicyDrpDwn)
      .find("button")
      .click({ force: true });
    return this;
  }

  checkTextIsNotInTable(text: string) {
    cy.get("table").should("not.have.text", text);
    return this;
  }

  shouldReloadJSONPolicies() {
    cy.findByTestId(this.#jsonEditorSelectPolicies).check();
    cy.findByTestId(this.#jsonEditorReloadBtn).contains("Reload").click();
    cy.findByTestId(this.#jsonEditorSavePoliciesBtn).contains("Save");
    cy.findByTestId(this.#jsonEditorReloadBtn).contains("Reload");
  }

  goToLoginTab() {
    cy.findByTestId(this.loginTab).click();
    return this;
  }

  goToEmailTab() {
    cy.findByTestId(this.emailTab).click();
    return this;
  }

  goToThemesTab() {
    cy.findByTestId(this.themesTab).click();
    return this;
  }

  goToLocalizationTab() {
    cy.findByTestId(this.localizationTab).click();
    return this;
  }

  goToLocalizationLocalesSubTab() {
    cy.findByTestId(this.#localizationLocalesSubTab).click();
    return this;
  }

  goToLocalizationRealmOverridesSubTab() {
    cy.findByTestId(this.#localizationRealmOverridesSubTab).click();
    return this;
  }

  goToLocalizationEffectiveMessageBundlesSubTab() {
    cy.findByTestId(this.#localizationEffectiveMessageBundlesSubTab).click();
    return this;
  }

  goToSecurityDefensesTab() {
    cy.findByTestId(this.securityDefensesTab).click();
    return this;
  }

  saveSecurityDefensesHeaders() {
    cy.findByTestId(this.#securityDefensesHeadersSaveBtn).click();
  }

  saveSecurityDefensesBruteForce() {
    cy.findByTestId(this.#securityDefensesBruteForceSaveBtn).click();
  }

  goToSecurityDefensesHeadersTab() {
    cy.findByTestId(this.#securityDefensesHeadersTab).click();
    return this;
  }

  goToSecurityDefensesBruteForceTab() {
    cy.findByTestId(this.#securityDefensesBruteForceTab).click();
    return this;
  }

  goToSessionsTab() {
    cy.findByTestId(this.sessionsTab).click();
    return this;
  }

  goToTokensTab() {
    cy.findByTestId(this.tokensTab).click();
    return this;
  }

  goToClientProfileByNameLink(profileName: string) {
    cy.get(this.#clientProfileLink).contains(profileName).click();
    return this;
  }
}
