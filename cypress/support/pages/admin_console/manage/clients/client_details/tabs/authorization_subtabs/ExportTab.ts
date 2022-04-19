import grantClipboardAccess from "../../../../../../../util/grantClipboardAccess";
import CommonPage from "../../../../../../CommonPage";

export default class ExportTab extends CommonPage {
  private exportDownloadBtn = "authorization-export-download";
  private exportCopyBtn = "authorization-export-copy";

  copy() {
    grantClipboardAccess();
    cy.findByTestId(this.exportCopyBtn).click();
    return this;
  }

  export() {
    cy.findByTestId(this.exportDownloadBtn).click();
    return this;
  }
}
