import TablePage from "../pages/admin_console/components/TablePage";
import CommonElements from "../pages/CommonElements";

export default class ModalUtils extends CommonElements {
  private modalTitle = ".pf-c-modal-box .pf-c-modal-box__title-text";
  private modalMessage = ".pf-c-modal-box .pf-c-modal-box__body";
  private confirmModalBtn = "confirm";
  private cancelModalBtn = "cancel";
  private closeModalBtn = ".pf-c-modal-box .pf-m-plain";
  private copyToClipboardBtn = '[id*="copy-button"]';
  private addModalDropdownBtn = "#add-dropdown > button";
  private addModalDropdownItem = "#add-dropdown [role='menuitem']";
  private tablePage = new TablePage(TablePage.tableSelector);

  constructor() {
    super(".pf-c-modal-box");
  }

  table() {
    return this.tablePage;
  }

  add() {
    cy.get(this.primaryBtn).contains("Add").click();
    return this;
  }

  confirmModal() {
    cy.findByTestId(this.confirmModalBtn).click({ force: true });

    return this;
  }

  checkConfirmButtonText(text: string) {
    cy.findByTestId(this.confirmModalBtn).contains(text);

    return this;
  }

  confirmModalWithItem(itemName: string) {
    cy.get(this.addModalDropdownBtn).click();
    cy.get(this.addModalDropdownItem).contains(itemName).click();

    return this;
  }

  cancelModal() {
    cy.findByTestId(this.cancelModalBtn).click({ force: true });

    return this;
  }

  cancelButtonContains(text: string) {
    cy.findByTestId(this.cancelModalBtn).contains(text);

    return this;
  }

  copyToClipboard() {
    cy.get(this.copyToClipboardBtn).click();

    return this;
  }

  closeModal() {
    cy.get(this.closeModalBtn).click({ force: true });

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
