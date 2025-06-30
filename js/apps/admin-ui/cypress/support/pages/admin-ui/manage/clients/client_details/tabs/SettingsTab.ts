import PageObject from "../../../../components/PageObject";
import Masthead from "../../../../Masthead";

export enum NameIdFormat {
  Username = "username",
  Email = "email",
  Transient = "transient",
  Persistent = "persistent",
}

const masthead = new Masthead();

export default class SettingsTab extends PageObject {
  #samlNameIdFormat = "#saml_name_id_format";
  #forceNameIdFormat = "attributes.saml_force_name_id_format";
  #forcePostBinding = "attributes.saml🍺force🍺post🍺binding";
  #forceArtifactBinding = "attributes.saml🍺artifact🍺binding";
  #includeAuthnStatement = "attributes.saml🍺authnstatement";
  #includeOneTimeUseCondition = "attributes.saml🍺onetimeuse🍺condition";
  #optimizeLookup = "attributes.saml🍺server🍺signature🍺keyinfo🍺ext";

  #signDocumentsSwitch = "attributes.saml🍺server🍺signature";
  #signAssertionsSwitch = "attributes.saml🍺assertion🍺signature";
  #signatureAlgorithm = "#saml🍺signature🍺algorithm";
  #signatureKeyName =
    "#saml🍺server🍺signature🍺keyinfo🍺xmlSigKeyInfoKeyNameTransformer";
  #canonicalization = "#saml_signature_canonicalization_method";

  #loginTheme = "#login_theme";
  #consentSwitch = "#consentRequired";
  #displayClientSwitch = "attributes.display🍺on🍺consent🍺screen";
  #consentScreenText = "attributes.consent🍺screen🍺text";

  #saveBtn = "settings-save";
  #revertBtn = "settingsRevert";

  #redirectUris = "redirectUris";

  #idpInitiatedSsoUrlName = "attributes.saml_idp_initiated_sso_url_name";
  #idpInitiatedSsoRelayState = "attributes.saml_idp_initiated_sso_relay_state";
  #masterSamlProcessingUrl = "adminUrl";

  public clickSaveBtn() {
    cy.findByTestId(this.#saveBtn).click();
    return this;
  }

  public clickRevertBtn() {
    cy.findByTestId(this.#revertBtn).click();
    return this;
  }

  public selectNameIdFormatDropdown(nameId: NameIdFormat) {
    cy.get(this.#samlNameIdFormat).click();
    cy.findByText(nameId).click();
    return this;
  }

  public selectSignatureAlgorithmDropdown(sign: string) {
    cy.get(this.#signatureAlgorithm).click();
    cy.findByText(sign).click();
    return this;
  }

  public selectSignatureKeyNameDropdown(keyName: string) {
    cy.get(this.#signatureKeyName).click();
    cy.findByText(keyName).click();
    return this;
  }

  public selectCanonicalizationDropdown(canon: string) {
    cy.get(this.#canonicalization).click();
    cy.findByText(canon).click();
  }

  public selectLoginThemeDropdown(theme: string) {
    cy.get(this.#loginTheme).click();
    cy.findByText(theme).click();
  }

  public clickForceNameIdFormatSwitch() {
    cy.findByTestId(this.#forceNameIdFormat).parent().click();
    return this;
  }

  public clickForcePostBindingSwitch() {
    cy.findByTestId(this.#forcePostBinding).parent().click();
    return this;
  }

  public clickForceArtifactBindingSwitch() {
    cy.findByTestId(this.#forceArtifactBinding).parent().click();
    return this;
  }

  public clickIncludeAuthnStatementSwitch() {
    cy.findByTestId(this.#includeAuthnStatement).parent().click();
    return this;
  }

  public clickIncludeOneTimeUseConditionSwitch() {
    cy.findByTestId(this.#includeOneTimeUseCondition).parent().click();
    return this;
  }

  public clickOptimizeLookupSwitch() {
    cy.findByTestId(this.#optimizeLookup).parent().click();
    return this;
  }

  public clickSignDocumentsSwitch() {
    cy.findByTestId(this.#signDocumentsSwitch).parent().click();
    return this;
  }

  public clickSignAssertionsSwitch() {
    cy.findByTestId(this.#signAssertionsSwitch).parent().click();
    return this;
  }

  public clickConsentSwitch() {
    cy.get(this.#consentSwitch).parent().click();
    return this;
  }

  public clickDisplayClientSwitch() {
    cy.findByTestId(this.#displayClientSwitch).parent().click();
    return this;
  }

  public assertNameIdFormatDropdown() {
    this.selectNameIdFormatDropdown(NameIdFormat.Email);
    this.selectNameIdFormatDropdown(NameIdFormat.Username);
    this.selectNameIdFormatDropdown(NameIdFormat.Persistent);
    this.selectNameIdFormatDropdown(NameIdFormat.Transient);
    return this;
  }

  public assertSignatureAlgorithmDropdown() {
    this.selectSignatureAlgorithmDropdown("RSA_SHA1");
    this.selectSignatureAlgorithmDropdown("RSA_SHA256");
    this.selectSignatureAlgorithmDropdown("RSA_SHA256_MGF1");
    this.selectSignatureAlgorithmDropdown("RSA_SHA512");
    this.selectSignatureAlgorithmDropdown("RSA_SHA512_MGF1");
    this.selectSignatureAlgorithmDropdown("DSA_SHA1");
    return this;
  }

  public assertSignatureKeyNameDropdown() {
    this.selectSignatureKeyNameDropdown("KEY_ID");
    this.selectSignatureKeyNameDropdown("CERT_SUBJECT");
    this.selectSignatureKeyNameDropdown("NONE");
    return this;
  }

  public assertCanonicalizationDropdown() {
    this.selectCanonicalizationDropdown("EXCLUSIVE_WITH_COMMENTS");
    this.selectCanonicalizationDropdown("EXCLUSIVE");
    this.selectCanonicalizationDropdown("INCLUSIVE_WITH_COMMENTS");
    this.selectCanonicalizationDropdown("INCLUSIVE");
    return this;
  }

  public assertLoginThemeDropdown() {
    this.selectLoginThemeDropdown("base");
    this.selectLoginThemeDropdown("keycloak");
    return this;
  }

  public assertSAMLCapabilitiesSwitches() {
    this.clickForceNameIdFormatSwitch();
    this.assertSwitchStateOn(cy.findByTestId(this.#forceNameIdFormat));

    this.clickForcePostBindingSwitch();
    this.assertSwitchStateOff(cy.findByTestId(this.#forcePostBinding));

    this.clickForceArtifactBindingSwitch();
    this.assertSwitchStateOn(cy.findByTestId(this.#forceArtifactBinding));

    this.clickIncludeAuthnStatementSwitch();
    this.assertSwitchStateOff(cy.findByTestId(this.#includeAuthnStatement));

    this.clickIncludeOneTimeUseConditionSwitch();
    this.assertSwitchStateOn(cy.findByTestId(this.#includeOneTimeUseCondition));

    this.clickOptimizeLookupSwitch();
    this.assertSwitchStateOn(cy.findByTestId(this.#optimizeLookup));
    return this;
  }

  public assertSignatureEncryptionSwitches() {
    cy.get(this.#signatureAlgorithm).should("exist");

    this.clickSignDocumentsSwitch();
    this.assertSwitchStateOff(cy.findByTestId(this.#signDocumentsSwitch));
    cy.get(this.#signatureAlgorithm).should("not.exist");

    this.clickSignAssertionsSwitch();
    this.assertSwitchStateOn(cy.findByTestId(this.#signAssertionsSwitch));
    cy.get(this.#signatureAlgorithm).should("exist");
    return this;
  }

  public assertLoginSettings() {
    cy.findByTestId(this.#displayClientSwitch).should("be.disabled");
    cy.findByTestId(this.#consentScreenText).should("be.disabled");
    this.clickConsentSwitch();
    cy.findByTestId(this.#displayClientSwitch).should("not.be.disabled");
    this.clickDisplayClientSwitch();
    cy.findByTestId(this.#consentScreenText).should("not.be.disabled");
    cy.findByTestId(this.#consentScreenText).click();
    cy.findByTestId(this.#consentScreenText).type("Consent Screen Text");
    return this;
  }

  public selectRedirectUriTextField(number: number, text: string) {
    cy.findByTestId(this.#redirectUris + number)
      .click()
      .clear()
      .type(text);
    return this;
  }

  public assertAccessSettings() {
    const redirectUriError =
      /Client could not be updated:.*(Master SAML Processing URL is not a valid URL|A redirect URI is not a valid URI).*/i;

    cy.findByTestId(this.#idpInitiatedSsoUrlName).click().type("a");
    cy.findByTestId(this.#idpInitiatedSsoRelayState).click().type("b");
    cy.findByTestId(this.#masterSamlProcessingUrl).click().type("c");

    this.selectRedirectUriTextField(0, "Redirect Uri");
    cy.findByText("Add valid redirect URIs").click();
    this.selectRedirectUriTextField(1, "Redirect Uri second field");
    this.clickSaveBtn();
    masthead.checkNotificationMessage(redirectUriError);

    return this;
  }
}
