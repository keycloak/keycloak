import PageObject from "../../components/PageObject";
import ListingPage from "../../ListingPage";
import Masthead from "../../Masthead";
import SidebarPage from "../../SidebarPage";
import GroupModal from "./GroupModal";
import MoveGroupModal from "./MoveGroupModal";

const groupModal = new GroupModal();
const moveGroupModal = new MoveGroupModal();
const listingPage = new ListingPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();

export default class GroupPage extends PageObject {
  protected createGroupEmptyStateBtn = "no-groups-in-this-realm-empty-action";
  private createGroupBtn = "openCreateGroupModal";
  protected actionDrpDwnButton = "action-dropdown";
  private actionDrpDwnItemSearchGroup = "searchGroup";

  public openCreateGroupModal(emptyState: boolean) {
    if (emptyState) {
      cy.findByTestId(this.createGroupEmptyStateBtn).click();
    } else {
      cy.findByTestId(this.createGroupBtn).click();
    }
    return this;
  }

  public createGroup(groupName: string, emptyState: boolean) {
    this.openCreateGroupModal(emptyState);
    groupModal
      .assertCreateGroupModalVisible(true)
      .setGroupNameInput(groupName)
      .create();
    cy.intercept("POST", "*/admin/realms/master/groups").as("post");
    return this;
  }

  public searchGroup(groupName: string, wait: boolean = false) {
    listingPage.searchItem(groupName, wait);
    return this;
  }

  public goToGroupChildGroupsTab(groupName: string) {
    listingPage.goToItemDetails(groupName);
    cy.intercept("GET", "*/admin/realms/master/groups/*").as("get");
    sidebarPage.waitForPageLoad();
    return this;
  }

  public selectGroupItemCheckbox(items: string[]) {
    for (const item of items) {
      listingPage.clickItemCheckbox(item);
    }
    return this;
  }

  public selectGroupItemCheckboxAllRows() {
    listingPage.clickTableHeaderItemCheckboxAllRows();
    return this;
  }

  public deleteSelectedGroups(confirmModal = true) {
    this.clickToolbarAction("Delete");
    if (confirmModal) {
      groupModal.confirmModal();
    }
    return this;
  }

  public deleteGroupItem(groupName: string, confirmModal = true) {
    listingPage.deleteItem(groupName);
    if (confirmModal) {
      groupModal.confirmModal();
    }
    return this;
  }

  public moveGroupItemAction(
    groupName: string,
    destinationGroupName: string[]
  ) {
    listingPage.clickRowDetails(groupName);
    listingPage.clickDetailMenu("Move to");
    moveGroupModal
      .assertModalVisible(true)
      .assertModalTitleEqual(`Move ${groupName} to root`);
    if (!destinationGroupName.includes("root")) {
      for (const destination of destinationGroupName) {
        moveGroupModal
          .clickRow(destination)
          .assertModalTitleEqual(`Move ${groupName} to ${destination}`);
      }
    }
    moveGroupModal.clickMove();
    this.assertNotificationGroupMoved();
    moveGroupModal.assertModalExist(false);
    return this;
  }

  public clickBreadcrumbItem(groupName: string) {
    super.clickBreadcrumbItem(groupName);
    return this;
  }

  public assertGroupItemExist(groupName: string, exist: boolean) {
    listingPage.itemExist(groupName, exist);
    return this;
  }

  public assertNoGroupsInThisRealmEmptyStateMessageExist(exist: boolean) {
    this.assertEmptyStateExist(exist);
    return this;
  }

  public assertGroupItemsEqual(number: number) {
    listingPage.itemsEqualTo(number);
    return this;
  }

  public assertNoSearchResultsMessageExist(exist: boolean) {
    super.assertEmptyStateExist(exist);
    return this;
  }

  public assertNotificationGroupDeleted() {
    masthead.checkNotificationMessage("Group deleted");
    return this;
  }

  public assertNotificationGroupsDeleted() {
    masthead.checkNotificationMessage("Groups deleted");
    return this;
  }

  public assertNotificationGroupCreated() {
    masthead.checkNotificationMessage("Group created");
    return this;
  }

  public assertNotificationGroupMoved() {
    masthead.checkNotificationMessage("Group moved");
    return this;
  }

  public assertNotificationGroupUpdated() {
    masthead.checkNotificationMessage("Group updated");
    return this;
  }

  public assertNotificationCouldNotCreateGroupWithEmptyName() {
    masthead.checkNotificationMessage(
      "Could not create group Group name is missing"
    );
    return this;
  }

  public assertNotificationCouldNotCreateGroupWithDuplicatedName(
    groupName: string
  ) {
    masthead.checkNotificationMessage(
      "Could not create group Top level group named '" +
        groupName +
        "' already exists."
    );
    return this;
  }
}
