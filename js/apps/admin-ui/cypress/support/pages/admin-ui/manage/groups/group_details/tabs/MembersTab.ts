import ModalUtils from "../../../../../../util/ModalUtils";
import ListingPage from "../../../../ListingPage";
import Masthead from "../../../../Masthead";
import SidebarPage from "../../../../SidebarPage";
import GroupDetailPage from "../GroupDetailPage";

const modalUtils = new ModalUtils();
const listingPage = new ListingPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();

export default class MembersTab extends GroupDetailPage {
  #addMemberEmptyStateBtn = "no-users-found-empty-action";
  #addMemberBtn = "addMember";
  #includeSubGroupsCheck = "includeSubGroupsCheck";

  public openAddMemberModal(emptyState: boolean) {
    cy.intercept("GET", "*/admin/realms/master/users?first=*").as("get");
    if (emptyState) {
      cy.findByTestId(this.#addMemberEmptyStateBtn).click();
    } else {
      cy.findByTestId(this.#addMemberBtn).click();
    }
    sidebarPage.waitForPageLoad();
    return this;
  }

  public addMember(usernames: string[], emptyState: boolean) {
    this.openAddMemberModal(emptyState);
    modalUtils.assertModalVisible(true).assertModalTitleEqual("Add member");
    for (const username of usernames) {
      listingPage.clickItemCheckbox(username);
    }
    modalUtils.add();
    modalUtils.assertModalExist(false);
    return this;
  }

  public selectUserItemCheckbox(items: string[]) {
    for (const item of items) {
      listingPage.clickItemCheckbox(item);
    }
    return this;
  }

  public leaveGroupSelectedUsers() {
    this.clickToolbarAction("Leave");
    return this;
  }

  public leaveGroupUserItem(username: string) {
    listingPage.clickRowDetails(username);
    listingPage.clickDetailMenu("Leave");
    return this;
  }

  public clickCheckboxIncludeSubGroupUsers() {
    cy.findByTestId(this.#includeSubGroupsCheck).click();
    return this;
  }

  public assertNotificationUserAddedToTheGroup(amount: number) {
    masthead.checkNotificationMessage(
      `${amount} user${amount > 1 ? "s" : ""} added to the group`,
    );
    return this;
  }

  public assertNotificationUserLeftTheGroup(amount: number) {
    masthead.checkNotificationMessage(
      `${amount} user${amount > 1 ? "s" : ""} left the group`,
    );
    return this;
  }

  public assertNoUsersFoundEmptyStateMessageExist(exist: boolean) {
    super.assertEmptyStateExist(exist);
    return this;
  }

  public assertUserItemExist(username: string, exist: boolean) {
    listingPage.itemExist(username, exist);
    return this;
  }
}
