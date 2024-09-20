import FormValidation from "../../../../forms/FormValidation";
import PageObject from "../../components/PageObject";
import Masthead from "../../Masthead";

const masthead = new Masthead();

export default class ProviderBaseGeneralSettingsPage extends PageObject {
  #redirectUriGroup = ".pf-v5-c-clipboard-copy__group";
  protected clientIdInput = "config.clientId";
  protected clientSecretInput = "config.clientSecret";
  #displayOrderInput = "#kc-display-order";
  #addBtn = "createProvider";
  #cancelBtn = "cancel";
  #requiredFieldErrorMsg = ".pf-v5-c-form__helper-text.pf-m-error";
  protected requiredFields: string[] = [
    this.clientIdInput,
    this.clientSecretInput,
  ];
  protected testData = {
    ClientId: "client",
    ClientSecret: "client_secret",
    DisplayOrder: "0",
  };

  public typeClientId(clientId: string) {
    if (clientId) {
      cy.findByTestId(this.clientIdInput).type(clientId);
    } else {
      cy.findByTestId(this.clientIdInput).clear();
    }
    return this;
  }

  public typeClientSecret(clientSecret: string) {
    if (clientSecret) {
      cy.findByTestId(this.clientSecretInput).type(clientSecret);
    } else {
      cy.findByTestId(this.clientSecretInput).clear();
    }
    return this;
  }

  public typeDisplayOrder(displayOrder: string) {
    cy.get(this.#displayOrderInput).type(displayOrder).blur();

    return this;
  }

  public clickShowPassword() {
    cy.findByTestId(this.clientSecretInput).parent().find("button").click();
    return this;
  }

  public clickCopyToClipboard() {
    cy.get(this.#redirectUriGroup).find("button").click();
    return this;
  }

  public clickAdd() {
    cy.findByTestId(this.#addBtn).click();
    return this;
  }

  public clickCancel() {
    cy.findByTestId(this.#cancelBtn).click();
    return this;
  }

  public assertRedirectUriInputEqual(value: string) {
    cy.get(this.#redirectUriGroup).find("input").should("have.value", value);
    return this;
  }

  public assertClientIdInputEqual(text: string) {
    cy.findByTestId(this.clientIdInput).should("have.text", text);
    return this;
  }

  public assertClientSecretInputEqual(text: string) {
    cy.findByTestId(this.clientSecretInput).should("have.text", text);
    return this;
  }

  public assertDisplayOrderInputEqual(text: string) {
    cy.findByTestId(this.clientSecretInput).should("have.text", text);
    return this;
  }

  public assertNotificationIdpCreated() {
    masthead.checkNotificationMessage("Identity provider successfully created");
    return this;
  }

  protected assertCommonRequiredFields(requiredFields: string[]) {
    requiredFields.forEach((elementLocator) => {
      if (elementLocator.includes("#")) {
        FormValidation.assertRequired(cy.get(elementLocator));
      } else {
        FormValidation.assertRequired(cy.findByTestId(elementLocator));
      }
    });
    return this;
  }

  public assertRequiredFieldsErrorsExist() {
    return this.assertCommonRequiredFields(this.requiredFields);
  }

  protected fillCommonFields(idpName: string) {
    this.typeClientId(this.testData["ClientId"] + idpName);
    this.typeClientSecret(this.testData["ClientSecret"] + idpName);
    this.typeDisplayOrder(this.testData["DisplayOrder"]);

    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);

    return this;
  }

  protected assertCommonFilledDataEqual(idpName: string) {
    cy.findByTestId(this.clientIdInput).should(
      "have.value",
      this.testData["ClientId"] + idpName,
    );
    cy.findByTestId(this.clientSecretInput).should("contain.value", "****");
    cy.get(this.#displayOrderInput).should(
      "have.value",
      this.testData["DisplayOrder"],
    );
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    return this;
  }
}
