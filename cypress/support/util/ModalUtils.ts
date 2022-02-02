export default class ModalUtils {
  private modalTitle = ".pf-c-modal-box .pf-c-modal-box__title-text";
  private modalMessage = ".pf-c-modal-box .pf-c-modal-box__body";

  private confirmModalBtn = "confirm";
  private cancelModalBtn = "cancel";
  private closeModalBtn = ".pf-c-modal-box .pf-m-plain";

  confirmModal(force = false) {
    cy.findByTestId(this.confirmModalBtn).click({ force });

    return this;
  }

  cancelModal() {
    cy.findByTestId(this.cancelModalBtn).click();

    return this;
  }

  closeModal() {
    cy.get(this.closeModalBtn).click();

    return this;
  }

  checkModalTitle(title: string) {
    cy.get(this.modalTitle).invoke("text").should("eq", title);

    return this;
  }

  checkModalMessage(message: string) {
    cy.get(this.modalMessage).invoke("text").should("eq", message);

    return this;
  }
}
