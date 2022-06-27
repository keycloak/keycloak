import ModalUtils from "../../../../../util/ModalUtils";
import GroupPage from "../GroupPage";

const modalUtils = new ModalUtils();

export default class GroupDetailPage extends GroupPage {
  private groupNamesColumn = '[data-label="Group name"] > a';
  private memberTab = "members";
  private childGroupsTab = "groups";
  private attributesTab = "attributes";
  private roleMappingTab = "role-mapping-tab";
  private memberNameColumn =
    '[data-testid="members-table"] > tbody > tr > [data-label="Name"]';
  private addMembers = "addMember";
  private memberUsernameColumn = 'tbody > tr > [data-label="Username"]';
  private actionDrpDwnItemRenameGroup = "renameGroupAction";
  private actionDrpDwnItemDeleteGroup = "deleteGroup";
  private headerGroupName = ".pf-l-level.pf-m-gutter";
  private renameGroupModalGroupNameInput = "groupNameInput";
  private renameGroupModalRenameBtn = "renameGroup";

  public goToChildGroupsTab() {
    cy.findByTestId(this.childGroupsTab).click();
    return this;
  }

  public goToMembersTab() {
    cy.findByTestId(this.memberTab).click();
    return this;
  }

  public goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();
    return this;
  }

  public goToRoleMappingTab() {
    cy.findByTestId(this.roleMappingTab).click();
    return this;
  }

  public headerActionRenameGroup() {
    super.openDropdownMenu("", cy.findByTestId(this.actionDrpDwnButton));
    super.clickDropdownMenuItem(
      "",
      cy.findByTestId(this.actionDrpDwnItemRenameGroup)
    );
    return this;
  }

  public headerActionDeleteGroup() {
    super.openDropdownMenu("", cy.findByTestId(this.actionDrpDwnButton));
    super.clickDropdownMenuItem(
      "",
      cy.findByTestId(this.actionDrpDwnItemDeleteGroup)
    );
    return this;
  }

  public renameGroup(newGroupName: string) {
    this.headerActionRenameGroup();
    modalUtils.checkModalTitle("Rename group");
    cy.findByTestId(this.renameGroupModalGroupNameInput)
      .clear()
      .type(newGroupName);
    cy.findByTestId(this.renameGroupModalRenameBtn).click();
    return this;
  }

  public deleteGroupHeaderAction() {
    this.headerActionDeleteGroup();
    return this;
  }

  public assertHeaderGroupNameEqual(groupName: string) {
    cy.get(this.headerGroupName).find("h1").should("have.text", groupName);
    return this;
  }

  checkListSubGroup(subGroups: string[]) {
    cy.get(this.groupNamesColumn).should((groups) => {
      expect(groups).to.have.length(subGroups.length);
      for (let index = 0; index < subGroups.length; index++) {
        const subGroup = subGroups[index];
        expect(groups).to.contain(subGroup);
      }
    });
    return this;
  }

  clickMembersTab() {
    cy.findByTestId(this.memberTab).click();
    return this;
  }

  checkListMembers(members: string[]) {
    cy.get(this.memberNameColumn).should((member) => {
      expect(member).to.have.length(members.length);
      for (let index = 0; index < members.length; index++) {
        expect(member.eq(index)).to.contain(members[index]);
      }
    });
    return this;
  }

  checkSelectableMembers(members: string[]) {
    cy.get(this.memberUsernameColumn).should((member) => {
      for (const user of members) {
        expect(member).to.contain(user);
      }
    });
    return this;
  }

  selectUsers(users: string[]) {
    for (const user of users) {
      cy.get(this.memberUsernameColumn)
        .contains(user)
        .parent()
        .find("input")
        .click();
    }
    return this;
  }

  clickAddMembers() {
    cy.findByTestId(this.addMembers).click();
    return this;
  }
}
