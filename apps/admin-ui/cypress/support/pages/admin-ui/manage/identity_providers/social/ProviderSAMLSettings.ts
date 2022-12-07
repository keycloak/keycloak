import PageObject from "../../../components/PageObject";
import Masthead from "../../../Masthead";

const masthead = new Masthead();

export default class ProviderSAMLSettings extends PageObject {
  private samlSwitch = "#Saml-switch";
  private modalConfirm = "#modal-confirm";
  private serviceProviderEntityID = "serviceProviderEntityId";
  private identityProviderEntityId = "identityProviderEntityId";
  private ssoServiceUrl = "sso-service-url";
  private singleLogoutServiceUrl = "single-logout-service-url";
  private nameIdPolicyFormat = "#kc-nameIdPolicyFormat";
  private principalType = "#kc-principalType";
  private principalAttribute = "principalAttribute";
  private principalSubjectNameId = "subjectNameId-option";
  private principalAttributeName = "attributeName-option";
  private principalFriendlyAttribute = "attributeFriendlyName-option";

  private transientPolicy = "transient-option";
  private emailPolicy = "email-option";
  private kerberosPolicy = "kerberos-option";
  private x509Policy = "x509-option";
  private windowsDomainQNPolicy = "windowsDomainQN-option";
  private unspecifiedPolicy = "unspecified-option";
  private persistentPolicy = "persistent-option";

  private allowCreate = "#allowCreate";
  private httpPostBindingResponse = "#httpPostBindingResponse";
  private httpPostBindingAuthnRequest = "#httpPostBindingAuthnRequest";
  private httpPostBindingLogout = "#httpPostBindingLogout";
  private wantAuthnRequestsSigned = "#wantAuthnRequestsSigned";

  private signatureAlgorithm = "#kc-signatureAlgorithm";
  private samlSignatureKeyName = "#kc-samlSignatureKeyName";

  private wantAssertionsSigned = "#wantAssertionsSigned";
  private wantAssertionsEncrypted = "#wantAssertionsEncrypted";
  private forceAuthentication = "#forceAuthentication";
  private validateSignature = "#validateSignature";
  private validatingX509Certs = "validatingX509Certs";
  private signServiceProviderMetadata = "#signServiceProviderMetadata";
  private passSubject = "#passSubject";
  private allowedClockSkew = "allowedClockSkew";
  private attributeConsumingServiceIndex = "attributeConsumingServiceIndex";
  private attributeConsumingServiceName = "attributeConsumingServiceName";

  private comparison = "#comparison";
  private saveBtn = "save";
  private revertBtn = "revert";

  public clickSaveBtn() {
    cy.findByTestId(this.saveBtn).click();
  }

  public clickRevertBtn() {
    cy.findByTestId(this.revertBtn).click();
  }

  public enableProviderSwitch() {
    cy.get(this.samlSwitch).parent().click();
    masthead.checkNotificationMessage("Provider successfully updated");
  }

  public disableProviderSwitch() {
    cy.get(this.samlSwitch).parent().click();
    cy.get(this.modalConfirm).click();
    masthead.checkNotificationMessage("Provider successfully updated");
  }

  public typeServiceProviderEntityId(entityId: string) {
    cy.findByTestId(this.serviceProviderEntityID)
      .click()
      .clear()
      .type(entityId);
    return this;
  }

  public typeIdentityProviderEntityId(entityId: string) {
    cy.findByTestId(this.identityProviderEntityId)
      .click()
      .clear()
      .type(entityId);
    return this;
  }

  public typeSsoServiceUrl(url: string) {
    cy.findByTestId(this.ssoServiceUrl).click().clear().type(url);
    return this;
  }

  public typeSingleLogoutServiceUrl(url: string) {
    cy.findByTestId(this.singleLogoutServiceUrl).click().clear().type(url);
    return this;
  }

  public typeX509Certs(cert: string) {
    cy.findByTestId(this.validatingX509Certs).click().clear().type(cert);
    return this;
  }

  public selectNamePolicyIdFormat() {
    cy.get(this.nameIdPolicyFormat).scrollIntoView().click();
  }

  public selectPrincipalFormat() {
    cy.get(this.principalType).scrollIntoView().click();
  }

  public selectSignatureAlgorithm(algorithm: string) {
    cy.get(this.signatureAlgorithm).scrollIntoView().click();
    cy.findByText(algorithm).click();
  }

  public selectSAMLSignature(key: string) {
    cy.get(this.samlSignatureKeyName).scrollIntoView().click();
    cy.findByText(key).click();
  }

  public selectComparison(comparison: string) {
    cy.get(this.comparison).scrollIntoView().click();
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
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.transientPolicy).click();
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.emailPolicy).click();
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.kerberosPolicy).click();
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.x509Policy).click();
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.windowsDomainQNPolicy).click();
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.unspecifiedPolicy).click();
    this.selectNamePolicyIdFormat();
    cy.findByTestId(this.persistentPolicy).click();
    return this;
  }

  public assertSignatureAlgorithm() {
    cy.get(this.wantAuthnRequestsSigned).parent().click();
    cy.get(this.signatureAlgorithm).should("not.exist");
    cy.get(this.samlSignatureKeyName).should("not.exist");
    this.clickRevertBtn();
    cy.get(this.signatureAlgorithm).should("exist");
    cy.get(this.samlSignatureKeyName).should("exist");

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
    this.selectPrincipalFormat();
    cy.findByTestId(this.principalAttributeName).click();
    cy.findByTestId(this.principalAttribute).should("exist").scrollIntoView();
    this.selectPrincipalFormat();
    cy.findByTestId(this.principalFriendlyAttribute).click();
    cy.findByTestId(this.principalAttribute).should("exist");
    this.selectPrincipalFormat();
    cy.findByTestId(this.principalSubjectNameId).click();
    cy.findByTestId(this.principalAttribute).should("not.exist");
    return this;
  }

  public assertSAMLSwitches() {
    cy.get(this.allowCreate).parent().click();
    cy.get(this.httpPostBindingResponse).parent().click();
    cy.get(this.httpPostBindingLogout).parent().click();
    cy.get(this.httpPostBindingAuthnRequest).parent().click();

    cy.get(this.wantAssertionsSigned).parent().click();
    cy.get(this.wantAssertionsEncrypted).parent().click();
    cy.get(this.forceAuthentication).parent().click();

    cy.get(this.signServiceProviderMetadata).parent().click();
    cy.get(this.passSubject).parent().click();

    return this;
  }

  public assertValidateSignatures() {
    cy.get(this.validateSignature).parent().click();
    cy.findByTestId(this.validatingX509Certs).should("not.exist");
    cy.get(this.validateSignature).parent().click();
    this.typeX509Certs("X509 Certificate");
    this.clickRevertBtn();
    cy.findByTestId(this.validatingX509Certs);
    this.clickSaveBtn();
    return this;
  }

  public assertTextFields() {
    cy.findByTestId(this.allowedClockSkew)
      .click()
      .type("not a number")
      .should("be.empty")
      .type("111");

    cy.findByTestId(this.attributeConsumingServiceIndex)
      .click()
      .type("not a number")
      .should("be.empty")
      .type("111");

    cy.findByTestId(this.attributeConsumingServiceName).click().type("name");
  }

  public assertAuthnContext() {
    this.selectComparison("minimum");
    this.selectComparison("maximum");
    this.selectComparison("better");
    this.selectComparison("exact");
    return this;
  }
}
