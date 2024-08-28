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
  #createGroupBtn = "openCreateGroupModal";
  protected actionDrpDwnButton = "action-dropdown";
  #searchField = "[data-testid='group-search']";

  openCreateGroupModal(emptyState: boolean) {
    if (emptyState) {
      cy.findByTestId(this.createGroupEmptyStateBtn).click();
    } else {
      cy.findByTestId(this.#createGroupBtn).click();
    }
    return this;
  }

  createGroup(groupName: string, emptyState: boolean) {
    this.openCreateGroupModal(emptyState);
    groupModal
      .assertCreateGroupModalVisible(true)
      .setGroupNameInput(groupName)
      .create();
    cy.intercept("POST", "*/admin/realms/master/groups").as("post");
    return this;
  }

  public searchGroup(searchValue: string, wait: boolean = false) {
    this.search(this.#searchField, searchValue, wait);

    return this;
  }

  protected search(
    searchField: string,
    searchValue: string,
    wait: boolean,
    exact = true,
  ) {
    if (wait) {
      const searchUrl = `/admin/realms/master/**/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }

    if (exact) {
      cy.findByTestId("exact-search").check();
    } else {
      cy.findByTestId("exact-search").uncheck();
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

  goToGroupChildGroupsTab(groupName: string) {
    listingPage.goToItemDetails(groupName);
    cy.intercept("GET", "*/admin/realms/master/groups/*").as("get");
    sidebarPage.waitForPageLoad();
    return this;
  }

  selectGroupItemCheckbox(items: string[]) {
    for (const item of items) {
      listingPage.clickItemCheckbox(item);
    }
    return this;
  }

  selectGroupItemCheckboxAllRows() {
    listingPage.clickTableHeaderItemCheckboxAllRows();
    return this;
  }

  deleteSelectedGroups(confirmModal = true) {
    this.clickToolbarAction("Delete");
    if (confirmModal) {
      groupModal.confirmModal();
    }
    return this;
  }

  showDeleteSelectedGroupsDialog() {
    this.clickToolbarAction("Delete");
    return this;
  }

  deleteGroupItem(groupName: string, confirmModal = true) {
    listingPage.deleteItem(groupName);
    if (confirmModal) {
      groupModal.confirmModal();
    }
    return this;
  }

  duplicateGroupItem(groupName: string, confirmModal = true) {
    listingPage.duplicateItem(groupName);
    if (confirmModal) {
      groupModal.confirmDuplicateModal();
    }
    return this;
  }

  moveGroupItemAction(groupName: string, destinationGroupName: string[]) {
    listingPage.clickRowDetails(groupName);
    listingPage.clickDetailMenu("Move to");
    moveGroupModal
      .assertModalVisible(true)
      .assertModalTitleEqual(`Move ${groupName} to Root`);
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

  clickBreadcrumbItem(groupName: string) {
    super.clickBreadcrumbItem(groupName);
    return this;
  }

  assertGroupItemExist(groupName: string, exist: boolean) {
    listingPage.itemExist(groupName, exist);
    return this;
  }

  assertNoGroupsInThisRealmEmptyStateMessageExist(exist: boolean) {
    this.assertEmptyStateExist(exist);
    return this;
  }

  assertGroupItemsEqual(number: number) {
    listingPage.itemsEqualTo(number);
    return this;
  }

  assertNoSearchResultsMessageExist(exist: boolean) {
    if (!exist) {
      cy.get("keycloak_groups_treeview").should("be.visible");
    } else {
      cy.get("keycloak_groups_treeview").should("not.exist");
    }
    return this;
  }

  assertNotificationGroupDeleted() {
    masthead.checkNotificationMessage("Group deleted");
    return this;
  }

  assertNotificationGroupsDeleted() {
    masthead.checkNotificationMessage("Groups deleted");
    return this;
  }

  assertNotificationGroupCreated() {
    masthead.checkNotificationMessage("Group created");
    return this;
  }

  assertNotificationGroupMoved() {
    masthead.checkNotificationMessage("Group moved");
    return this;
  }

  assertNotificationGroupUpdated() {
    masthead.checkNotificationMessage("Group updated");
    return this;
  }

  assertNotificationCouldNotCreateGroupWithEmptyName() {
    masthead.checkNotificationMessage(
      "Could not create group Group name is missing",
    );
    return this;
  }

  assertNotificationCouldNotCreateGroupWithDuplicatedName(groupName: string) {
    masthead.checkNotificationMessage(
      "Could not create group Top level group named '" +
        groupName +
        "' already exists.",
    );
    return this;
  }

  assertNotificationGroupDuplicated() {
    masthead.checkNotificationMessage("Group duplicated");
    return this;
  }

  goToGroupActions(groupName: string) {
    listingPage.clickRowDetails(groupName);

    return this;
  }
}
