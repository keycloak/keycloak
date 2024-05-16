import Select from "../../../../../forms/Select";
import PageObject from "../../../components/PageObject";
import Masthead from "../../../Masthead";

const masthead = new Masthead();

export default class ProviderSAMLSettings extends PageObject {
  #samlSwitch = "Saml-switch";
  #modalConfirm = "#modal-confirm";
  #serviceProviderEntityID = "config.entityId";
  #identityProviderEntityId = "identityProviderEntityId";
  #ssoServiceUrl = "config.singleSignOnServiceUrl";
  #singleLogoutServiceUrl = "config.singleLogoutServiceUrl";
  #nameIdPolicyFormat = "#nameIDPolicyFormat";
  #principalType = "#principalType";

  #allowCreate = "config.allowCreate";
  #httpPostBindingResponse = "config.postBindingResponse";
  #httpPostBindingAuthnRequest = "config.postBindingAuthnRequest";
  #httpPostBindingLogout = "config.postBindingLogout";
  #wantAuthnRequestsSigned = "config.wantAuthnRequestsSigned";

  #signatureAlgorithm = "#signatureAlgorithm";
  #samlSignatureKeyName = "#xmlSigKeyInfoKeyNameTransformer";

  #wantAssertionsSigned = "config.wantAssertionsSigned";
  #wantAssertionsEncrypted = "config.wantAssertionsEncrypted";
  #forceAuthentication = "config.forceAuthn";
  #validateSignature = "config.validateSignature";
  #validatingX509Certs = "config.signingCertificate";
  #signServiceProviderMetadata = "config.signSpMetadata";
  #passSubject = "config.loginHint";
  #allowedClockSkew = "#config\\.allowedClockSkew";
  #attributeConsumingServiceIndex = "#config\\.attributeConsumingServiceIndex";
  #attributeConsumingServiceName = "config.attributeConsumingServiceName";

  #comparison = "#comparison";
  #saveBtn = "idp-details-save";
  #revertBtn = "idp-details-revert";

  public clickSaveBtn() {
    cy.findByTestId(this.#saveBtn).click();
  }

  public clickRevertBtn() {
    cy.findByTestId(this.#revertBtn).click();
  }

  public enableProviderSwitch() {
    cy.findByTestId(this.#samlSwitch).parent().click();
    masthead.checkNotificationMessage("Provider successfully updated");
  }

  public disableProviderSwitch() {
    cy.findByTestId(this.#samlSwitch).parent().click();
    cy.get(this.#modalConfirm).click();
    masthead.checkNotificationMessage("Provider successfully updated");
  }

  public typeServiceProviderEntityId(entityId: string) {
    cy.findByTestId(this.#serviceProviderEntityID)
      .click()
      .clear()
      .type(entityId);
    return this;
  }

  public typeIdentityProviderEntityId(entityId: string) {
    cy.findByTestId(this.#identityProviderEntityId)
      .click()
      .clear()
      .type(entityId);
    return this;
  }

  public typeSsoServiceUrl(url: string) {
    cy.findByTestId(this.#ssoServiceUrl).clear().type(url);
    return this;
  }

  public typeSingleLogoutServiceUrl(url: string) {
    cy.findByTestId(this.#singleLogoutServiceUrl).clear().type(url);
    return this;
  }

  public typeX509Certs(cert: string) {
    cy.findByTestId(this.#validatingX509Certs).clear();
    cy.findByTestId(this.#validatingX509Certs).type(cert);
    return this;
  }

  public selectNamePolicyIdFormat(option: string) {
    cy.get(this.#nameIdPolicyFormat).scrollIntoView();
    Select.selectItem(cy.get(this.#nameIdPolicyFormat), option);
    Select.assertSelectedItem(cy.get(this.#nameIdPolicyFormat), option);
    return this;
  }

  public selectPrincipalFormat(option: string) {
    cy.get(this.#principalType).scrollIntoView();
    Select.selectItem(cy.get(this.#principalType), option);
    Select.assertSelectedItem(cy.get(this.#principalType), option);
    return this;
  }

  public selectSignatureAlgorithm(algorithm: string) {
    cy.get(this.#signatureAlgorithm).scrollIntoView();
    Select.selectItem(cy.get(this.#signatureAlgorithm), algorithm);
  }

  public selectSAMLSignature(key: string) {
    cy.get(this.#samlSignatureKeyName).scrollIntoView();
    Select.selectItem(cy.get(this.#samlSignatureKeyName), key);
  }

  public selectComparison(comparison: string) {
    cy.get(this.#comparison).scrollIntoView().click();
    cy.findByText(comparison).scrollIntoView().click();
  }

  public assertIdAndURLFields() {
    const ssoServiceUrlError =
      "Could not update the provider The url [singleSignOnServiceUrl] is malformed";
    const singleLogoutServiceUrlError =
      "Could not update the provider The url [singleLogoutServiceUrl] is malformed";
    this.typeServiceProviderEntityId("ServiceProviderEntityId");
    this.typeIdentityProviderEntityId("IdentityProviderEntityId");
    this.clickSaveBtn();

    this.typeSsoServiceUrl("Not a real URL");
    this.clickSaveBtn();
    masthead.checkNotificationMessage(ssoServiceUrlError);
    this.clickRevertBtn();

    this.typeSingleLogoutServiceUrl("Not a real URL");
    this.clickSaveBtn();
    masthead.checkNotificationMessage(singleLogoutServiceUrlError);
    return this;
  }

  public assertNameIdPolicyFormat() {
    this.selectNamePolicyIdFormat("Transient");
    this.selectNamePolicyIdFormat("Email");
    this.selectNamePolicyIdFormat("Kerberos");
    this.selectNamePolicyIdFormat("X.509 Subject Name");
    this.selectNamePolicyIdFormat("Windows Domain Qualified Name");
    this.selectNamePolicyIdFormat("Unspecified");
    this.selectNamePolicyIdFormat("Persistent");
    return this;
  }

  public assertSignatureAlgorithm() {
    cy.findByTestId(this.#wantAuthnRequestsSigned).parent().click();
    cy.get(this.#signatureAlgorithm).should("not.exist");
    cy.get(this.#samlSignatureKeyName).should("not.exist");
    this.clickRevertBtn();
    cy.get(this.#signatureAlgorithm).should("exist");
    cy.get(this.#samlSignatureKeyName).should("exist");

    this.selectSignatureAlgorithm("RSA_SHA1");
    this.selectSignatureAlgorithm("RSA_SHA256");
    this.selectSignatureAlgorithm("RSA_SHA256_MGF1");
    this.selectSignatureAlgorithm("RSA_SHA512");
    this.selectSignatureAlgorithm("RSA_SHA512_MGF1");
    this.selectSignatureAlgorithm("DSA_SHA1");

    this.selectSAMLSignature("NONE");
    this.selectSAMLSignature("KEY_ID");
    this.selectSAMLSignature("CERT_SUBJECT");

    return this;
  }

  public assertPrincipalType() {
    this.selectPrincipalFormat("Subject NameID");
    this.selectPrincipalFormat("Attribute [Name]");
    this.selectPrincipalFormat("Attribute [Friendly Name]");
    return this;
  }

  public assertSAMLSwitches() {
    cy.findByTestId(this.#allowCreate).parent().click();
    cy.findByTestId(this.#httpPostBindingResponse).parent().click();
    cy.findByTestId(this.#httpPostBindingLogout).parent().click();
    cy.findByTestId(this.#httpPostBindingAuthnRequest).parent().click();

    cy.findByTestId(this.#wantAssertionsSigned).parent().click();
    cy.findByTestId(this.#wantAssertionsEncrypted).parent().click();
    cy.findByTestId(this.#forceAuthentication).parent().click();

    cy.findByTestId(this.#signServiceProviderMetadata).parent().click();
    cy.findByTestId(this.#passSubject).parent().click();

    return this;
  }

  public assertValidateSignatures() {
    cy.findByTestId(this.#validateSignature).parent().click();
    cy.findByTestId(this.#validatingX509Certs).should("not.exist");
    cy.findByTestId(this.#validateSignature).parent().click();
    this.typeX509Certs("X509 Certificate");
    this.clickRevertBtn();
    cy.findByTestId(this.#validatingX509Certs);
    this.clickSaveBtn();
    return this;
  }

  public assertTextFields() {
    cy.get(this.#allowedClockSkew)
      .find("input")
      .should("have.value", 0)
      .clear()
      .type("111");

    cy.get(this.#attributeConsumingServiceIndex)
      .find("input")
      .should("have.value", 0)
      .clear()
      .type("111");

    cy.findByTestId(this.#attributeConsumingServiceName).click().type("name");
  }

  public assertAuthnContext() {
    this.selectComparison("minimum");
    this.selectComparison("maximum");
    this.selectComparison("better");
    this.selectComparison("exact");
    return this;
  }
}
