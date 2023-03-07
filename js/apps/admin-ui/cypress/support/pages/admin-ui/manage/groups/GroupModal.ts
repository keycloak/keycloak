import ModalUtils from "../../../../util/ModalUtils";

export default class GroupModal extends ModalUtils {
  private createGroupModalTitle = "Create a group";
  private groupNameInput = "groupNameInput";
  private createGroupBnt = "createGroup";
  private renameButton = "renameGroup";

  public setGroupNameInput(name: string) {
    cy.findByTestId(this.groupNameInput).clear().type(name);
    return this;
  }

  public create() {
    cy.findByTestId(this.createGroupBnt).click();
    return this;
  }

  public rename() {
    cy.findByTestId(this.renameButton).click();
    return this;
  }

  public assertCreateGroupModalVisible(isVisible: boolean) {
    super
      .assertModalVisible(isVisible)
      .assertModalTitleEqual(this.createGroupModalTitle);
    return this;
  }
}
