export default class CredentialsPage {
  readonly #credentialsTab = "credentials";
  readonly #emptyStatePasswordBtn = "no-credentials-empty-action";
  readonly #emptyStateResetBtn = "credential-reset-empty-action";
  readonly #resetBtn = "credentialResetBtn";
  readonly #setPasswordBtn = "confirm";
  readonly #credentialResetModal = "credential-reset-modal";
  readonly #resetModalActionsToggleBtn =
    "[data-testid=credential-reset-modal] #actions";

  readonly #passwordField = "passwordField";
  readonly #passwordConfirmationField = "passwordConfirmationField";
  readonly #resetActions = [
    "Verify Email",
    "Update Profile",
    "Configure OTP",
    "Update Password",
    "Terms and Conditions",
  ];
  readonly #confirmationButton = "confirm";
  readonly #editLabelBtn = "editUserLabelBtn";
  readonly #labelField = "userLabelFld";
  readonly #editConfirmationBtn = "editUserLabelAcceptBtn";
  readonly #showDataDialogBtn = "showDataBtn";
  readonly #closeDataDialogBtn = '.pf-v5-c-modal-box [aria-label^="Close"]';

  goToCredentialsTab() {
    cy.intercept("/admin/realms/*/users/*/credentials").as("load");
    cy.findByTestId(this.#credentialsTab).click();
    cy.wait("@load");
    cy.wait(200);

    return this;
  }
  clickEmptyStatePasswordBtn() {
    cy.findByTestId(this.#emptyStatePasswordBtn).click();

    return this;
  }

  clickEmptyStateResetBtn() {
    cy.findByTestId(this.#emptyStateResetBtn).click();

    return this;
  }

  clickResetBtn() {
    cy.findByTestId(this.#resetBtn).click();

    return this;
  }

  clickResetModalActionsToggleBtn() {
    cy.get(this.#resetModalActionsToggleBtn).click();

    return this;
  }

  clickResetModalAction(index: number) {
    cy.get("[data-testid=credential-reset-modal] .pf-v5-c-menu__list")
      .contains(this.#resetActions[index])
      .click();

    return this;
  }

  clickConfirmationBtn() {
    cy.findByTestId(this.#confirmationButton).click();

    return this;
  }

  fillPasswordForm() {
    cy.findByTestId(this.#passwordField).type("test");
    cy.findByTestId(this.#passwordConfirmationField).type("test");

    return this;
  }

  fillResetCredentialForm() {
    cy.findByTestId(this.#credentialResetModal);
    this.clickResetModalActionsToggleBtn()
      .clickResetModalAction(2)
      .clickResetModalAction(3)
      .clickConfirmationBtn();

    return this;
  }

  clickSetPasswordBtn() {
    cy.findByTestId(this.#setPasswordBtn).click();

    return this;
  }

  clickEditCredentialLabelBtn() {
    cy.findByTestId(this.#editLabelBtn)
      .should("be.visible")
      .click({ force: true });

    return this;
  }

  fillEditCredentialForm() {
    cy.findByTestId(this.#labelField).focus().type("test");

    return this;
  }

  clickEditConfirmationBtn() {
    cy.findByTestId(this.#editConfirmationBtn).click();

    return this;
  }

  clickShowDataDialogBtn() {
    cy.findByTestId(this.#showDataDialogBtn).click();

    return this;
  }

  clickCloseDataDialogBtn() {
    cy.get(this.#closeDataDialogBtn).click({ force: true });

    return this;
  }
}
