export default class CredentialsPage {
  private readonly credentialsTab = "credentials";
  private readonly emptyStatePasswordBtn = "no-credentials-empty-action";
  private readonly emptyStateResetBtn = "credential-reset-empty-action";
  private readonly resetBtn = "credentialResetBtn";
  private readonly setPasswordBtn = "confirm";
  private readonly credentialResetModal = "credential-reset-modal";
  private readonly resetModalActionsToggleBtn =
    "[data-testid=credential-reset-modal] #actions";
  private readonly passwordField = "passwordField";
  private readonly passwordConfirmationField = "passwordConfirmationField";
  private readonly resetActions = [
    "VERIFY_EMAIL-option",
    "UPDATE_PROFILE-option",
    "CONFIGURE_TOTP-option",
    "UPDATE_PASSWORD-option",
    "terms_and_conditions-option",
  ];
  private readonly confirmationButton = "confirm";
  private readonly editLabelBtn = "editUserLabelBtn";
  private readonly labelField = "userLabelFld";
  private readonly editConfirmationBtn = "editUserLabelAcceptBtn";
  private readonly showDataDialogBtn = "showDataBtn";
  private readonly closeDataDialogBtn = '.pf-c-modal-box [aria-label^="Close"]';

  goToCredentialsTab() {
    cy.intercept("/admin/realms/*/users/*/credentials").as("load");
    cy.findByTestId(this.credentialsTab).click();
    cy.wait("@load");
    cy.wait(200);

    return this;
  }
  clickEmptyStatePasswordBtn() {
    cy.findByTestId(this.emptyStatePasswordBtn).click();

    return this;
  }

  clickEmptyStateResetBtn() {
    cy.findByTestId(this.emptyStateResetBtn).click();

    return this;
  }

  clickResetBtn() {
    cy.findByTestId(this.resetBtn).click();

    return this;
  }

  clickResetModalActionsToggleBtn() {
    cy.get(this.resetModalActionsToggleBtn).click();

    return this;
  }

  clickResetModalAction(index: number) {
    cy.findByTestId(this.resetActions[index]).click();

    return this;
  }

  clickConfirmationBtn() {
    cy.findByTestId(this.confirmationButton).dblclick();

    return this;
  }

  fillPasswordForm() {
    cy.findByTestId(this.passwordField).type("test");
    cy.findByTestId(this.passwordConfirmationField).type("test");

    return this;
  }

  fillResetCredentialForm() {
    cy.findByTestId(this.credentialResetModal);
    this.clickResetModalActionsToggleBtn()
      .clickResetModalAction(2)
      .clickResetModalAction(3)
      .clickConfirmationBtn();

    return this;
  }

  clickSetPasswordBtn() {
    cy.findByTestId(this.setPasswordBtn).click();

    return this;
  }

  clickEditCredentialLabelBtn() {
    cy.findByTestId(this.editLabelBtn)
      .should("be.visible")
      .click({ force: true });

    return this;
  }

  fillEditCredentialForm() {
    cy.findByTestId(this.labelField).focus().type("test");

    return this;
  }

  clickEditConfirmationBtn() {
    cy.findByTestId(this.editConfirmationBtn).click();

    return this;
  }

  clickShowDataDialogBtn() {
    cy.findByTestId(this.showDataDialogBtn).click();

    return this;
  }

  clickCloseDataDialogBtn() {
    cy.get(this.closeDataDialogBtn).click({ force: true });

    return this;
  }
}
