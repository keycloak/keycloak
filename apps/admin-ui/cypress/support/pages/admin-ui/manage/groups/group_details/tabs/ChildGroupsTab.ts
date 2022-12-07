import GroupDetailPage from "../GroupDetailPage";

export default class ChildGroupsTab extends GroupDetailPage {
  protected createGroupEmptyStateBtn =
    "no-groups-in-this-sub-group-empty-action";

  public assertNoGroupsInThisSubGroupEmptyStateMessageExist(exist: boolean) {
    super.assertEmptyStateExist(exist);
    return this;
  }
}
