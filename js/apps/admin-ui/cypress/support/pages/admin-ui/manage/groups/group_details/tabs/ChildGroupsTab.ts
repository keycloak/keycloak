import GroupDetailPage from "../GroupDetailPage";

export default class ChildGroupsTab extends GroupDetailPage {
  protected createGroupEmptyStateBtn =
    "no-groups-in-this-sub-group-empty-action";
  protected createSubGroupBtn = "openCreateGroupModal";

  public assertNoGroupsInThisSubGroupEmptyStateMessageExist(exist: boolean) {
    super.assertEmptyStateExist(exist);
    return this;
  }

  public openCreateSubGroupModal(emptyState: boolean) {
    if (emptyState) {
      cy.findByTestId(this.createGroupEmptyStateBtn).click();
    } else {
      cy.findByTestId(this.createSubGroupBtn).click();
    }
    return this;
  }
}
