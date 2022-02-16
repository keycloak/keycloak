export default class UserProfile {
  private userProfileTab = "rs-user-profile-tab";
  private attributesTab = "attributesTab";
  private attributesGroupTab = "attributesGroupTab";
  private jsonEditorTab = "jsonEditorTab";
  private createAttributeButton = "createAttributeBtn";
  private actionsDrpDwn = "actions-dropdown";
  private deleteDrpDwnOption = "deleteDropdownAttributeItem";
  private editDrpDwnOption = "editDropdownAttributeItem";

  goToTab() {
    cy.findByTestId(this.userProfileTab).click();
    return this;
  }

  goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();
    return this;
  }

  goToAttributesGroupTab() {
    cy.findByTestId(this.attributesGroupTab).click();
    return this;
  }

  goToJsonEditorTab() {
    cy.findByTestId(this.jsonEditorTab).click();
    return this;
  }

  createAttributeButtonClick() {
    cy.findByTestId(this.createAttributeButton).click();
    return this;
  }

  selectDropdown() {
    cy.findByTestId(this.actionsDrpDwn).click();
    return this;
  }

  selectDeleteOption() {
    cy.findByTestId(this.deleteDrpDwnOption).click();
    return this;
  }

  selectEditOption() {
    cy.findByTestId(this.editDrpDwnOption).click();
    return this;
  }
}
