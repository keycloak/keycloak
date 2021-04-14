export default class GroupModal {
  private openPartialImport = "openPartialImportModal";

  open() {
    cy.getId(this.openPartialImport).click();
    return this;
  }
}
