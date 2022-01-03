import ListingPage from "../../ListingPage";

const expect = chai.expect;
export default class RealmSettingsPage {
  generalSaveBtn = "general-tab-save";
  themesSaveBtn = "themes-tab-save";
  loginTab = "rs-login-tab";
  userProfileTab = "rs-user-profile-tab";
  selectLoginTheme = "#kc-login-theme";
  loginThemeList = "#kc-login-theme + ul";
  selectAccountTheme = "#kc-account-theme";
  accountThemeList = "#kc-account-theme + ul";
  selectAdminTheme = "#kc-admin-console-theme";
  adminThemeList = "#kc-admin-console-theme + ul";
  selectEmailTheme = "#kc-email-theme";
  emailThemeList = "#kc-email-theme + ul";
  hostInput = "#kc-host";
  ssoSessionIdleSelectMenu = "#kc-sso-session-idle-select-menu";
  ssoSessionIdleSelectMenuList = "#kc-sso-session-idle-select-menu > div > ul";
  ssoSessionMaxSelectMenu = "#kc-sso-session-max-select-menu";
  ssoSessionMaxSelectMenuList = "#kc-sso-session-max-select-menu > div > ul";

  ssoSessionMaxRememberMeSelectMenu =
    "#kc-sso-session-max-remember-me-select-menu";
  ssoSessionMaxRememberMeSelectMenuList =
    "#kc-sso-session-max-remember-me-select-menu > div > ul";

  ssoSessionIdleRememberMeSelectMenu =
    "#kc-sso-session-idle-remember-me-select-menu";
  ssoSessionIdleRememberMeSelectMenuList =
    "#kc-sso-session-idle-remember-me-select-menu > div > ul";

  clientSessionIdleSelectMenu = "#kc-client-session-idle-select-menu";
  clientSessionIdleSelectMenuList =
    "#kc-client-session-idle-select-menu > div > ul";

  clientSessionMaxSelectMenu = "#kc-client-session-max-select-menu";
  clientSessionMaxSelectMenuList =
    "#kc-client-session-max-select-menu > div > ul";

  offlineSessionIdleSelectMenu = "#kc-offline-session-idle-select-menu";

  loginTimeoutSelectMenu = "#kc-login-timeout-select-menu";
  loginTimeoutSelectMenuList = "#kc-login-timeout-select-menu > div > ul";

  loginActionTimeoutSelectMenu = "#kc-login-action-timeout-select-menu";
  loginActionTimeoutSelectMenuList =
    "#kc-login-action-timeout-select-menu > div > ul";

  selectDefaultLocale = "select-default-locale";
  defaultLocaleList = "select-default-locale > div > ul";
  supportedLocalesTypeahead =
    "#kc-l-supported-locales-select-multi-typeahead-typeahead";
  supportedLocalesToggle = "#kc-l-supported-locales";
  emailSaveBtn = "email-tab-save";
  managedAccessSwitch = "user-managed-access-switch";
  profileEnabledSwitch = "user-profile-enabled-switch";
  userRegSwitch = "user-reg-switch";
  forgotPwdSwitch = "forgot-pw-switch";
  rememberMeSwitch = "remember-me-switch";
  emailAsUsernameSwitch = "email-as-username-switch";
  loginWithEmailSwitch = "login-with-email-switch";
  duplicateEmailsSwitch = "duplicate-emails-switch";
  verifyEmailSwitch = "verify-email-switch";
  authSwitch = "email-authentication-switch";
  fromInput = "sender-email-address";
  enableSslCheck = "enable-ssl";
  enableStartTlsCheck = "enable-start-tls";
  addProviderDropdown = "addProviderDropdown";
  addProviderButton = "add-provider-button";
  displayName = "display-name-input";
  enableEvents = "eventsEnabled";
  eventsUserSave = "save-user";
  enableAdminEvents = "adminEventsEnabled";
  eventsAdminSave = "save-admin";
  eventTypeColumn = 'tbody > tr > [data-label="Event saved type"]';
  filterSelectMenu = ".kc-filter-type-select";
  passiveKeysOption = "passive-keys-option";
  disabledKeysOption = "disabled-keys-option";
  testConnectionButton = "test-connection-button";
  modalTestConnectionButton = "modal-test-connection-button";
  emailAddressInput = "email-address-input";
  addBundleButton = "add-bundle-button";
  confirmAddBundle = "add-bundle-confirm-button";
  keyInput = "key-input";
  valueInput = "value-input";
  deleteAction = "delete-action";
  modalConfirm = "modalConfirm";
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
    "#kc-access-token-lifespan-select-menu > div > ul";

  accessTokenLifespanImplicitSelectMenu =
    "#kc-access-token-lifespan-implicit-select-menu";
  accessTokenLifespanImplicitSelectMenuList =
    "#kc-access-token-lifespan-implicit-select-menu > div > ul";

  clientLoginTimeoutSelectMenu = "#kc-client-login-timeout-select-menu";
  clientLoginTimeoutSelectMenuList =
    "#kc-client-login-timeout-select-menu > div > ul";

  offlineSessionMaxSelectMenu = "#kc-offline-session-max-select-menu";
  offlineSessionMaxSelectMenuList =
    "#kc-offline-session-max-select-menu > div > ul";

  userInitiatedActionLifespanSelectMenu =
    "#kc-user-initiated-action-lifespan-select-menu";
  userInitiatedActionLifespanSelectMenuList =
    "#kc-user-initiated-action-lifespan-select-menu > div > ul";

  defaultAdminInitatedInputSelectMenu =
    "#kc-default-admin-initiated-select-menu";
  defaultAdminInitatedInputSelectMenuList =
    "#kc-default-admin-initiated-select-menu";

  emailVerificationSelectMenu = "#kc-email-verification-select-menu";
  emailVerificationSelectMenuList =
    "#kc-email-verification-select-menu > div > ul";

  idpEmailVerificationSelectMenu = "#kc-idp-email-verification-select-menu";
  idpEmailVerificationSelectMenuList =
    "#kc-idp-email-verification-select-menu > div > ul";

  forgotPasswordSelectMenu = "#kc-forgot-pw-select-menu";
  forgotPasswordSelectMenuList = "#kc-forgot-pw-select-menu > div > ul";

  executeActionsSelectMenu = "#kc-execute-actions-select-menu";
  executeActionsSelectMenuList = "#kc-execute-actions-select-menu > div > ul";

  private formViewProfilesView = "formView-profilesView";
  private jsonEditorProfilesView = "jsonEditor-profilesView";
  private createProfileBtn = "createProfile";
  private formViewSelect = "formView-profilesView";
  private jsonEditorSelect = "jsonEditor-profilesView";
  private formViewSelectPolicies = "formView-policiesView";
  private jsonEditorSelectPolicies = "jsonEditor-policiesView";
  private newClientProfileNameInput = "client-profile-name";
  private newClientProfileDescriptionInput = "client-profile-description";
  private saveNewClientProfileBtn = "saveCreateProfile";
  private cancelNewClientProfile = "cancelCreateProfile";
  private createPolicyEmptyStateBtn = "no-client-policies-empty-action";
  private createPolicyBtn = "createPolicy";
  private newClientPolicyNameInput = "client-policy-name";
  private newClientPolicyDescriptionInput = "client-policy-description";
  private saveNewClientPolicyBtn = "saveCreatePolicy";
  private cancelNewClientPolicyBtn = "cancelCreatePolicy";
  private alertMessage = ".pf-c-alert__title";
  private moreDrpDwn = ".pf-c-dropdown__toggle.pf-m-plain";
  private moreDrpDwnItems = ".pf-c-dropdown__menu-item";
  private deleteDialogTitle = ".pf-c-modal-box__title-text";
  private deleteDialogBodyText = ".pf-c-modal-box__body";
  private deleteDialogCancelBtn = ".pf-c-button.pf-m-link";
  private jsonEditorSaveBtn = "jsonEditor-saveBtn";
  private jsonEditorSavePoliciesBtn = "jsonEditor-policies-saveBtn";
  private jsonEditorReloadBtn = "jsonEditor-reloadBtn";
  private jsonEditor = ".monaco-scrollable-element.editor-scrollable.vs";
  private createClientDrpDwn = ".pf-c-dropdown.pf-m-align-right";
  private clientPolicyDrpDwn = "action-dropdown";
  private searchFld = "[id^=realm-settings][id$=profilesinput]";
  private searchFldPolicies = "[id^=realm-settings][id$=clientPoliciesinput]";
  private clientProfileOne =
    'a[href*="realm-settings/clientPolicies/Test/edit-profile"]';
  private clientProfileTwo =
    'a[href*="realm-settings/clientPolicies/Edit/edit-profile"]';
  private clientPolicy =
    'a[href*="realm-settings/clientPolicies/Test/edit-policy"]';
  private reloadBtn = "reloadProfile";
  private addExecutor = "addExecutor";
  private addExecutorDrpDwn = ".pf-c-select__toggle";
  private addExecutorDrpDwnOption = "executorType-select";
  private addExecutorCancelBtn = "addExecutor-cancelBtn";
  private addExecutorSaveBtn = "addExecutor-saveBtn";
  private availablePeriodExecutorFld = "available-period";
  private editExecutor = "editExecutor";

  private listingPage = new ListingPage();
  private addCondition = "addCondition";
  private addConditionDrpDwn = ".pf-c-select__toggle";
  private addConditionDrpDwnOption = "conditionType-select";
  private addConditionCancelBtn = "addCondition-cancelBtn";
  private addConditionSaveBtn = "addCondition-saveBtn";
  private conditionTypeLink = "condition-type-link";
  private clientRolesConditionLink = "client-roles-condition-link";
  private clientScopesConditionLink = "client-scopes-condition-link";
  private eventListenersFormLabel = ".pf-c-form__label-text";
  private eventListenersDrpDwn = ".pf-c-select.kc_eventListeners_select";
  private eventListenersSaveBtn = "saveEventListenerBtn";
  private eventListenersRevertBtn = "revertEventListenerBtn";
  private eventListenersInputFld =
    ".pf-c-form-control.pf-c-select__toggle-typeahead";
  private eventListenersDrpDwnOption = ".pf-c-select__menu-item";
  private eventListenersDrwDwnSelect =
    ".pf-c-button.pf-c-select__toggle-button.pf-m-plain";
  private eventListenerRemove = '[data-ouia-component-id="Remove"]';
  private roleSelect = ".pf-c-select.kc-role-select";
  private selectScopeButton = "select-scope-button";
  private deleteClientRolesCondition = "delete-client-roles-condition";
  private deleteClientScopesCondition = "delete-client-scopes-condition";

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
    cy.get(this.hostInput).clear().type(host);
    return this;
  }

  setDefaultLocale(locale: string) {
    cy.get(this.selectDefaultLocale).click();
    cy.get(this.defaultLocaleList).contains(locale).click();
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

  addSenderEmail(senderEmail: string) {
    cy.findByTestId(this.fromInput).clear();

    if (senderEmail) {
      cy.findByTestId(this.fromInput).type(senderEmail);
    }

    return this;
  }

  testSelectFilter() {
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.passiveKeysOption).click();
    cy.get(this.filterSelectMenu).first().click();
    cy.findByTestId(this.disabledKeysOption).click();
  }

  toggleSwitch(switchName: string) {
    cy.findByTestId(switchName).click({ force: true });

    return this;
  }

  toggleCheck(switchName: string) {
    cy.findByTestId(switchName).click();

    return this;
  }

  toggleAddProviderDropdown() {
    const keysUrl = "/auth/admin/realms/master/keys";
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

    cy.findByTestId(this.confirmAddBundle).click();

    return this;
  }

  deleteProvider(providerName: string) {
    cy.findAllByTestId("provider-name-link")
      .contains(providerName)
      .parent()
      .siblings(".pf-c-data-list__item-action")
      .click()
      .findByTestId(this.deleteAction)
      .click();
    cy.wait(500).findByTestId(this.modalConfirm).click();
  }

  enterConsoleDisplayName(name: string) {
    cy.findByTestId(this.displayName).clear().type(name);
  }

  save(saveBtn: string) {
    cy.findByTestId(saveBtn).click();

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
    listType: string
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
      this.ssoSessionIdleSelectMenuList
    );
    cy.findByTestId(this.ssoSessionMaxInput).clear().type("2");
    this.changeTimeUnit(
      "Hours",
      this.ssoSessionMaxSelectMenu,
      this.ssoSessionMaxSelectMenuList
    );
    cy.findByTestId(this.ssoSessionIdleRememberMeInput).clear().type("3");
    this.changeTimeUnit(
      "Days",
      this.ssoSessionIdleRememberMeSelectMenu,
      this.ssoSessionIdleRememberMeSelectMenuList
    );
    cy.findByTestId(this.ssoSessionMaxRememberMeInput).clear().type("4");
    this.changeTimeUnit(
      "Minutes",
      this.ssoSessionMaxRememberMeSelectMenu,
      this.ssoSessionMaxRememberMeSelectMenuList
    );

    cy.findByTestId(this.clientSessionIdleInput).clear().type("5");
    this.changeTimeUnit(
      "Hours",
      this.clientSessionIdleSelectMenu,
      this.clientSessionIdleSelectMenuList
    );
    cy.findByTestId(this.clientSessionMaxInput).clear().type("6");
    this.changeTimeUnit(
      "Days",
      this.clientSessionMaxSelectMenu,
      this.clientSessionMaxSelectMenuList
    );

    cy.findByTestId(this.offlineSessionIdleInput).clear().type("7");
    this.toggleSwitch(this.offlineSessionMaxSwitch);

    cy.findByTestId(this.loginTimeoutInput).clear().type("9");
    this.changeTimeUnit(
      "Minutes",
      this.loginTimeoutSelectMenu,
      this.loginTimeoutSelectMenuList
    );
    cy.findByTestId(this.loginActionTimeoutInput).clear().type("10");
    this.changeTimeUnit(
      "Days",
      this.loginActionTimeoutSelectMenu,
      this.loginActionTimeoutSelectMenuList
    );
  }

  populateTokensPage() {
    this.toggleSwitch(this.revokeRefreshTokenSwitch);

    cy.findByTestId(this.accessTokenLifespanInput)
      .focus()
      .clear({ force: true });
    cy.findByTestId(this.accessTokenLifespanInput).clear().type("1");
    this.changeTimeUnit(
      "Days",
      this.accessTokenLifespanSelectMenu,
      this.accessTokenLifespanSelectMenuList
    );
    cy.findByTestId(this.accessTokenLifespanImplicitInput).clear().type("2");
    this.changeTimeUnit(
      "Minutes",
      this.accessTokenLifespanImplicitSelectMenu,
      this.accessTokenLifespanImplicitSelectMenuList
    );

    cy.findByTestId(this.clientLoginTimeoutInput).clear().type("3");
    this.changeTimeUnit(
      "Hours",
      this.clientLoginTimeoutSelectMenu,
      this.clientLoginTimeoutSelectMenuList
    );

    cy.findByTestId(this.userInitiatedActionLifespanInput).clear().type("4");
    this.changeTimeUnit(
      "Minutes",
      this.userInitiatedActionLifespanSelectMenu,
      this.userInitiatedActionLifespanSelectMenuList
    );

    cy.findByTestId(this.defaultAdminInitatedInput).clear().type("5");
    this.changeTimeUnit(
      "Days",
      this.defaultAdminInitatedInputSelectMenu,
      this.defaultAdminInitatedInputSelectMenuList
    );

    cy.findByTestId(this.emailVerificationInput).clear().type("6");
    this.changeTimeUnit(
      "Days",
      this.emailVerificationSelectMenu,
      this.emailVerificationSelectMenuList
    );

    cy.findByTestId(this.idpEmailVerificationInput).clear().type("7");
    this.changeTimeUnit(
      "Days",
      this.idpEmailVerificationSelectMenu,
      this.idpEmailVerificationSelectMenuList
    );

    cy.findByTestId(this.forgotPasswordInput).clear().type("8");
    this.changeTimeUnit(
      "Days",
      this.forgotPasswordSelectMenu,
      this.forgotPasswordSelectMenuList
    );
    cy.findByTestId(this.executeActionsInput).clear().type("9");
    this.changeTimeUnit(
      "Days",
      this.executeActionsSelectMenu,
      this.executeActionsSelectMenuList
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

  clickAdd() {
    cy.findByTestId("addEventTypeConfirm").click();
    return this;
  }

  shouldDisplayEventListenersForm() {
    cy.get(this.eventListenersFormLabel)
      .should("be.visible")
      .contains("Event listeners");
    cy.get(this.eventListenersDrpDwn).should("exist");
    cy.findByTestId(this.eventListenersSaveBtn).should("exist");
    cy.findAllByTestId(this.eventListenersRevertBtn).should("exist");
  }

  shouldRevertSavingEventListener() {
    cy.get(this.eventListenersInputFld).click().type("email");
    cy.get(this.eventListenersDrpDwnOption).click();
    cy.get(this.eventListenersDrwDwnSelect).click();
    cy.findByTestId(this.eventListenersRevertBtn).click();
    cy.get(this.eventListenersDrpDwn).should("not.have.text", "email");
  }

  shouldSaveEventListener() {
    cy.get(this.eventListenersInputFld).click().type("email");
    cy.get(this.eventListenersDrpDwnOption).click();
    cy.get(this.eventListenersDrwDwnSelect).click();
    cy.findByTestId(this.eventListenersSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Event listener has been updated."
    );
  }

  shouldRemoveEventFromEventListener() {
    cy.get(this.eventListenerRemove).first().click();
    cy.findByTestId(this.eventListenersSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Event listener has been updated."
    );
    cy.get(this.eventListenersDrpDwn).should("not.have.text", "jboss-logging");
  }

  shouldRemoveAllEventListeners() {
    cy.get(".pf-c-button.pf-m-plain.pf-c-select__toggle-clear").click();
    cy.findByTestId(this.eventListenersSaveBtn).click();
    cy.get(this.eventListenersDrpDwn).should("not.have.text", "jboss-logging");
    cy.get(this.eventListenersDrpDwn).should("not.have.text", "email");
  }

  shouldReSaveEventListener() {
    cy.get(this.eventListenersInputFld).click().type("jboss-logging");
    cy.get(this.eventListenersDrpDwnOption).click();
    cy.get(this.eventListenersDrwDwnSelect).click();
    cy.findByTestId(this.eventListenersSaveBtn).click();
  }

  shouldDisplayProfilesTab() {
    cy.findByTestId(this.createProfileBtn).should("exist");
    cy.findByTestId(this.formViewSelect).should("exist");
    cy.findByTestId(this.jsonEditorSelect).should("exist");
    cy.get("table").should("be.visible").contains("td", "Global");
  }

  shouldDisplayNewClientProfileForm() {
    cy.findByTestId(this.createProfileBtn).click();
    cy.findByTestId(this.newClientProfileNameInput).should("exist");
    cy.findByTestId(this.newClientProfileDescriptionInput).should("exist");
    cy.findByTestId(this.saveNewClientProfileBtn).should("exist");
    cy.findByTestId(this.cancelNewClientProfile).should("exist");
  }

  shouldCompleteAndCancelCreateNewClientProfile() {
    cy.findByTestId(this.createProfileBtn).click();
    cy.findByTestId(this.newClientProfileNameInput).type("Test");
    cy.findByTestId(this.newClientProfileDescriptionInput).type(
      "Test Description"
    );
    cy.findByTestId(this.cancelNewClientProfile).click();
    cy.get("table").should("not.have.text", "Test");
  }

  shouldCompleteAndCreateNewClientProfile() {
    cy.findByTestId(this.createProfileBtn).click();
    cy.findByTestId(this.newClientProfileNameInput).type("Test");
    cy.findByTestId(this.newClientProfileDescriptionInput).type(
      "Test Description"
    );
    cy.findByTestId(this.saveNewClientProfileBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "New client profile created"
    );
  }

  shouldSearchClientProfile() {
    cy.get(this.searchFld).click({ force: true }).type("Test").click();
    cy.get("table").should("be.visible").contains("td", "Test");
    cy.get(this.searchFld).click({ force: true }).clear();
  }

  shouldDisplayDeleteClientPolicyDialog() {
    cy.get(this.moreDrpDwn).last().click();
    cy.get(this.moreDrpDwnItems).click();
    cy.get(this.deleteDialogTitle).contains("Delete policy?");
    cy.get(this.deleteDialogBodyText).contains(
      "This action will permanently delete the policy Test. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete");
    cy.get(this.deleteDialogCancelBtn).contains("Cancel").click();
    cy.get("table").should("not.have.text", "Test");
  }

  shouldDeleteClientProfileDialog() {
    this.listingPage.searchItem("Test", false);
    this.listingPage.clickRowDetails("Test").clickDetailMenu("Delete");
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get(this.alertMessage).should("be.visible", "Client profile deleted");
    cy.get("table").should("not.have.text", "Test");
  }

  shouldNavigateBetweenFormAndJSONView() {
    cy.findByTestId(this.jsonEditorProfilesView).check();
    cy.findByTestId(this.jsonEditorSaveBtn).contains("Save");
    cy.findByTestId(this.jsonEditorReloadBtn).contains("Reload");
    cy.findByTestId(this.formViewProfilesView).check();
    cy.findByTestId(this.createProfileBtn).contains("Create client profile");
  }

  shouldSaveChangedJSONProfiles() {
    cy.findByTestId(this.jsonEditorProfilesView).check();
    cy.get(this.jsonEditor).type(`{pageup}{del} [{
      "name": "Test",
      "description": "Test Description",
      "executors": [],
      "global": false
    }, {downarrow}{end}{backspace}{backspace}`);
    cy.findByTestId(this.jsonEditorSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "The client profiles configuration was updated"
    );
    cy.findByTestId(this.formViewProfilesView).check();
    cy.get("table").should("be.visible").contains("td", "Test");
  }

  shouldNotCreateDuplicateClientProfile() {
    cy.get(this.alertMessage).should(
      "be.visible",
      "Could not create client profile: 'proposed client profile name duplicated.'"
    );
  }

  shouldEditClientProfile() {
    cy.get(this.clientProfileOne).click();
    cy.findByTestId(this.newClientProfileNameInput)
      .click()
      .clear()
      .type("Edit");
    cy.findByTestId(this.newClientProfileDescriptionInput)
      .click()
      .clear()
      .type("Edit Description");
    cy.findByTestId(this.saveNewClientProfileBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Client profile updated successfully"
    );
  }

  shouldCheckEditedClientProfileListed() {
    cy.get("table").should("be.visible").contains("td", "Edit");
    cy.get("table").should("not.have.text", "Test");
  }

  shouldShowErrorWhenNameBlank() {
    cy.get(this.clientProfileTwo).click();
    cy.findByTestId(this.newClientProfileNameInput).click().clear();
    cy.get("form").should("not.have.text", "Required field");
  }

  shouldReloadClientProfileEdits() {
    cy.get(this.clientProfileTwo).click();
    cy.findByTestId(this.newClientProfileNameInput).type("Reloading");
    cy.findByTestId(this.reloadBtn).click();
    cy.findByTestId(this.newClientProfileNameInput).should(
      "have.value",
      "Edit"
    );
  }

  shouldNotHaveExecutorsConfigured() {
    cy.get(this.clientProfileTwo).click();
    cy.get('h6[class*="kc-emptyExecutors"]').should(
      "have.text",
      "No executors configured"
    );
  }

  shouldCancelAddingExecutor() {
    cy.get(this.clientProfileTwo).click();
    cy.findByTestId(this.addExecutor).click();
    cy.get(this.addExecutorDrpDwn).click();
    cy.findByTestId(this.addExecutorDrpDwnOption)
      .contains("secure-ciba-signed-authn-req")
      .click();
    cy.findByTestId(this.addExecutorCancelBtn).click();
    cy.get('h6[class*="kc-emptyExecutors"]').should(
      "have.text",
      "No executors configured"
    );
  }

  shouldAddExecutor() {
    cy.get(this.clientProfileTwo).click();
    cy.findByTestId(this.addExecutor).click();
    cy.get(this.addExecutorDrpDwn).click();
    cy.findByTestId(this.addExecutorDrpDwnOption)
      .contains("secure-ciba-signed-authn-req")
      .click();
    cy.findByTestId(this.addExecutorSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Success! Executor created successfully"
    );
    cy.get('ul[class*="pf-c-data-list"]').should(
      "have.text",
      "secure-ciba-signed-authn-req"
    );
  }

  shouldCancelDeletingExecutor() {
    cy.get(this.clientProfileTwo).click();
    cy.get('svg[class*="kc-executor-trash-icon"]').click();
    cy.get(this.deleteDialogTitle).contains("Delete executor?");
    cy.get(this.deleteDialogBodyText).contains(
      "The action will permanently delete secure-ciba-signed-authn-req. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete");
    cy.get(this.deleteDialogCancelBtn).contains("Cancel").click();
    cy.get('ul[class*="pf-c-data-list"]').should(
      "have.text",
      "secure-ciba-signed-authn-req"
    );
  }

  shouldCancelEditingExecutor() {
    cy.get(this.clientProfileTwo).click();

    cy.intercept("/auth/admin/realms/master/client-policies/profiles*").as(
      "profilesFetch"
    );
    cy.findByTestId(this.editExecutor).first().click();
    cy.wait("@profilesFetch");

    cy.findByTestId(this.addExecutorCancelBtn).click();
    cy.get('ul[class*="pf-c-data-list"]').should(
      "have.text",
      "secure-ciba-signed-authn-req"
    );
    cy.findByTestId(this.editExecutor).first().click();
    cy.findByTestId(this.availablePeriodExecutorFld).should(
      "have.value",
      "3600"
    );
  }

  shouldEditExecutor() {
    cy.get(this.clientProfileTwo).click();
    cy.findByTestId(this.editExecutor).first().click();
    cy.findByTestId(this.availablePeriodExecutorFld).clear().type("4000");
    cy.findByTestId(this.addExecutorSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Executor updated successfully"
    );
  }

  shouldDeleteExecutor() {
    cy.get(this.clientProfileTwo).click();
    cy.get('svg[class*="kc-executor-trash-icon"]').click();
    cy.get(this.deleteDialogTitle).contains("Delete executor?");
    cy.get(this.deleteDialogBodyText).contains(
      "The action will permanently delete secure-ciba-signed-authn-req. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get('h6[class*="kc-emptyExecutors"]').should(
      "have.text",
      "No executors configured"
    );
  }

  shouldDeleteEditedProfile() {
    cy.get(this.moreDrpDwn).last().click();
    cy.get(this.moreDrpDwnItems).click();
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get(this.alertMessage).should("be.visible", "Client profile deleted");
    cy.get("table").should("not.have.text", "Edit");
  }

  shouldNotCreateDuplicateClientPolicy() {
    cy.get(this.alertMessage).should(
      "be.visible",
      "Could not create client policy: 'proposed client policy name duplicated.'"
    );
  }

  shouldRemoveClientFromCreateView() {
    cy.findByTestId(this.createProfileBtn).click();
    cy.findByTestId(this.newClientProfileNameInput).type("Test again");
    cy.findByTestId(this.newClientProfileDescriptionInput).type(
      "Test Again Description"
    );
    cy.findByTestId(this.saveNewClientProfileBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "New client profile created"
    );
    cy.get(this.createClientDrpDwn).contains("Action").click();
    cy.findByTestId("deleteClientProfileDropdown").click();
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get(this.alertMessage).should("be.visible", "Client profile deleted");
    cy.get("table").should("not.have.text", "Test Again Description");
  }

  shouldReloadJSONProfiles() {
    cy.findByTestId(this.jsonEditorProfilesView).check();
    cy.findByTestId(this.jsonEditorReloadBtn).contains("Reload").click();
    cy.findByTestId(this.jsonEditorSaveBtn).contains("Save");
    cy.findByTestId(this.jsonEditorReloadBtn).contains("Reload");
  }

  shouldSaveChangedJSONPolicies() {
    cy.findByTestId(this.jsonEditorSelectPolicies).check();
    cy.findByTestId(this.jsonEditorReloadBtn).click();

    cy.get(this.jsonEditor).type(`{pageup}{del} [{
      "name": "Reload", 
    }, {downarrow}{end}{backspace}{backspace}{backspace}{backspace}`);

    cy.findByTestId(this.jsonEditorReloadBtn).click();

    cy.get(this.jsonEditor).type(`{pageup}{del} [{
      "name": "Test", 
      "description": "Test Description",
      "enabled": false,
      "conditions": [], 
      "profiles": [],
    }, {downarrow}{end}{backspace}{backspace}{backspace}{backspace}`);

    cy.findByTestId(this.jsonEditorSavePoliciesBtn).click();

    cy.get(this.alertMessage).should(
      "be.visible",
      "The client policy configuration was updated"
    );
    cy.findByTestId(this.formViewSelectPolicies).check();
    cy.get("table").should("be.visible").contains("td", "Test");
  }

  shouldNavigateBetweenFormAndJSONViewPolicies() {
    cy.findByTestId(this.jsonEditorSelectPolicies).check();
    cy.findByTestId(this.jsonEditorSavePoliciesBtn).contains("Save");
    cy.findByTestId(this.jsonEditorReloadBtn).contains("Reload");
    cy.findByTestId(this.formViewSelectPolicies).check();
    cy.findByTestId(this.createPolicyEmptyStateBtn).contains(
      "Create client policy"
    );
  }

  shouldDisplayPoliciesTab() {
    cy.findByTestId(this.createPolicyEmptyStateBtn).should("exist");
    cy.findByTestId(this.formViewSelectPolicies).should("exist");
    cy.findByTestId(this.jsonEditorSelectPolicies).should("exist");
  }

  shouldDisplayNewClientPolicyForm() {
    cy.findByTestId(this.createPolicyEmptyStateBtn).click();
    cy.findByTestId(this.newClientPolicyNameInput).should("exist");
    cy.findByTestId(this.newClientPolicyDescriptionInput).should("exist");
    cy.findByTestId(this.saveNewClientPolicyBtn).should("exist");
    cy.findByTestId(this.cancelNewClientPolicyBtn).should("exist");
  }

  shouldCompleteAndCancelCreateNewClientPolicy() {
    cy.findByTestId(this.createPolicyEmptyStateBtn).click();
    cy.findByTestId(this.newClientPolicyNameInput).type("Test");
    cy.findByTestId(this.newClientPolicyDescriptionInput).type(
      "Test Description"
    );
    cy.findByTestId(this.cancelNewClientPolicyBtn).click();
    cy.get("table").should("not.have.text", "Test");
  }

  shouldCompleteAndCreateNewClientPolicy() {
    cy.findByTestId(this.createPolicyBtn).click();
    cy.findByTestId(this.newClientPolicyNameInput).type("Test");
    cy.findByTestId(this.newClientPolicyDescriptionInput).type(
      "Test Description"
    );
    cy.findByTestId(this.saveNewClientPolicyBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "New client profile created"
    );
  }

  shouldCompleteAndCreateNewClientPolicyFromEmptyState() {
    cy.findByTestId(this.createPolicyEmptyStateBtn).click();
    cy.findByTestId(this.newClientPolicyNameInput).type("Test");
    cy.findByTestId(this.newClientPolicyDescriptionInput).type(
      "Test Description"
    );
    cy.findByTestId(this.saveNewClientPolicyBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "New client profile created"
    );
  }

  shouldSearchClientPolicy() {
    cy.get(this.searchFldPolicies).click({ force: true }).type("Test").click();
    cy.get("table").should("be.visible").contains("td", "Test");
    cy.get(this.searchFldPolicies).click({ force: true }).clear();
  }

  shouldDisplayDeleteClientProfileDialog() {
    this.listingPage.searchItem("Test", false);
    this.listingPage.clickRowDetails("Test").clickDetailMenu("Delete");
    cy.get(this.deleteDialogTitle).contains("Delete profile?");
    cy.get(this.deleteDialogBodyText).contains(
      "This action will permanently delete the profile Test. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete");
    cy.get(this.deleteDialogCancelBtn).contains("Cancel").click();
    cy.get("table").should("be.visible").contains("td", "Test");
  }

  shouldNotHaveConditionsConfigured() {
    cy.get(this.clientPolicy).click();
    cy.get('h6[class*="kc-emptyConditions"]').should(
      "have.text",
      "No conditions configured"
    );
  }

  shouldCancelAddingCondition() {
    cy.get(this.clientPolicy).click();
    cy.findByTestId(this.addCondition).click();
    cy.get(this.addConditionDrpDwn).click();
    cy.findByTestId(this.addConditionDrpDwnOption)
      .contains("any-client")
      .click();
    cy.findByTestId(this.addConditionCancelBtn).click();
    cy.get('h6[class*="kc-emptyConditions"]').should(
      "have.text",
      "No conditions configured"
    );
  }

  shouldAddClientRolesCondition() {
    cy.get(this.clientPolicy).click();
    cy.findByTestId(this.addCondition).click();
    cy.get(this.addConditionDrpDwn).click();
    cy.findByTestId(this.addConditionDrpDwnOption)
      .contains("client-roles")
      .click();
    cy.get(this.roleSelect).click().contains("impersonation").click();

    cy.get(this.roleSelect).contains("manage-realm").click();

    cy.get(this.roleSelect).contains("view-users").click();

    cy.get(this.roleSelect).click();

    cy.findByTestId(this.addConditionSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Success! Condition created successfully"
    );
    cy.get('ul[class*="pf-c-data-list"]').should("have.text", "client-roles");
  }

  addClientScopes() {
    cy.findByTestId(this.selectScopeButton).click();
    cy.get(".pf-c-table__check > input[name=checkrow0]").click();
    cy.get(".pf-c-table__check > input[name=checkrow1]").click();
    cy.get(".pf-c-table__check > input[name=checkrow2]").click();

    cy.findByTestId("modalConfirm").contains("Add").click();
  }

  shouldAddClientScopesCondition() {
    cy.get(this.clientPolicy).click();
    cy.findByTestId(this.addCondition).click();
    cy.get(this.addConditionDrpDwn).click();
    cy.findByTestId(this.addConditionDrpDwnOption)
      .contains("client-scopes")
      .click();

    this.addClientScopes();

    cy.findByTestId(this.addConditionSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Success! Condition created successfully"
    );
    cy.get('ul[class*="pf-c-data-list"]').contains("client-scopes");
  }

  shouldEditClientRolesCondition() {
    cy.get(this.clientPolicy).click();

    cy.findByTestId(this.clientRolesConditionLink).click();

    cy.get(this.roleSelect).click();
    cy.get(this.roleSelect).contains("create-client").click();

    cy.get(this.roleSelect).click();

    cy.findByTestId(this.addConditionSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Success! Condition updated successfully"
    );
  }

  shouldEditClientScopesCondition() {
    cy.get(this.clientPolicy).click();

    cy.findByTestId(this.clientScopesConditionLink).click();

    cy.wait(200);

    this.addClientScopes();

    cy.findByTestId(this.addConditionSaveBtn).click();
    cy.get(this.alertMessage).should(
      "be.visible",
      "Success! Condition updated successfully"
    );
  }

  shouldCancelDeletingCondition() {
    cy.get(this.clientPolicy).click();
    cy.findByTestId(this.deleteClientRolesCondition).click();
    cy.get(this.deleteDialogTitle).contains("Delete condition?");
    cy.get(this.deleteDialogBodyText).contains(
      "This action will permanently delete client-roles. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete");
    cy.get(this.deleteDialogCancelBtn).contains("Cancel").click();
    cy.get('ul[class*="pf-c-data-list"]').contains("client-roles");
  }

  shouldDeleteClientRolesCondition() {
    cy.get(this.clientPolicy).click();
    cy.findByTestId(this.deleteClientRolesCondition).click();
    cy.get(this.deleteDialogTitle).contains("Delete condition?");
    cy.get(this.deleteDialogBodyText).contains(
      "This action will permanently delete client-roles. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get('ul[class*="pf-c-data-list"]').contains("client-scopes");
  }

  shouldDeleteClientScopesCondition() {
    cy.get(this.clientPolicy).click();
    cy.findByTestId(this.deleteClientScopesCondition).click();
    cy.get(this.deleteDialogTitle).contains("Delete condition?");
    cy.get(this.deleteDialogBodyText).contains(
      "This action will permanently delete client-scopes. This cannot be undone."
    );
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get('h6[class*="kc-emptyConditions"]').should(
      "have.text",
      "No conditions configured"
    );
  }

  shouldDeleteClientPolicyDialog() {
    this.listingPage.searchItem("Test", false);
    this.listingPage.clickRowDetails("Test").clickDetailMenu("Delete");
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get(this.alertMessage).should("be.visible", "Client profile deleted");
    cy.get("table").should("not.have.text", "Test");
  }

  shouldRemoveClientPolicyFromCreateView() {
    cy.findByTestId(this.createPolicyEmptyStateBtn).click();
    cy.findByTestId(this.newClientPolicyNameInput).type("Test again");
    cy.findByTestId(this.newClientPolicyDescriptionInput).type(
      "Test Again Description"
    );
    cy.findByTestId(this.saveNewClientPolicyBtn).click();
    cy.get(this.alertMessage).should("be.visible", "New client policy created");
    cy.wait(200);
    cy.findByTestId(this.clientPolicyDrpDwn).contains("Action").click();
    cy.findByTestId("deleteClientPolicyDropdown").click();
    cy.findByTestId("modalConfirm").contains("Delete").click();
    cy.get(this.alertMessage).should("be.visible", "Client profile deleted");
    cy.get("table").should("not.have.text", "Test Again Description");
  }

  shouldReloadJSONPolicies() {
    cy.findByTestId(this.jsonEditorSelectPolicies).check();
    cy.findByTestId(this.jsonEditorReloadBtn).contains("Reload").click();
    cy.findByTestId(this.jsonEditorSavePoliciesBtn).contains("Save");
    cy.findByTestId(this.jsonEditorReloadBtn).contains("Reload");
  }
}
