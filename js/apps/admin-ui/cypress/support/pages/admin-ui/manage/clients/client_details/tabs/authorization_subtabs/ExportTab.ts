import grantClipboardAccess from "../../../../../../../util/grantClipboardAccess";
import CommonPage from "../../../../../../CommonPage";

export default class ExportTab extends CommonPage {
  #exportDownloadBtn = "authorization-export-download";
  #exportCopyBtn = "authorization-export-copy";

  copy() {
    grantClipboardAccess();
    cy.findByTestId(this.#exportCopyBtn).click();
    return this;
  }

  export() {
    cy.findByTestId(this.#exportDownloadBtn).click();
    return this;
  }
}
