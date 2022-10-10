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
  private searchField = "[data-testid='group-search']";

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

  public searchGroup(searchValue: string, wait: boolean = false) {
    this.search(this.searchField, searchValue, wait);

    return this;
  }

  protected search(searchField: string, searchValue: string, wait: boolean) {
    if (wait) {
      const searchUrl = `/admin/realms/master/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }

    cy.get(searchField + " input").clear();
    if (searchValue) {
      cy.get(searchField + " input").type(searchValue);
      cy.get(searchField + " button[type='submit']").click({ force: true });
    } else {
      // TODO: Remove else and move clickSearchButton outside of the if
      cy.get(searchField).type("{enter}");
    }

    if (wait) {
      cy.wait(["@search"]);
    }
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
