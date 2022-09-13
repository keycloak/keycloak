const expect = chai.expect;
export default class RoleMappingTab {
  private type = "client";
  private tab = "serviceAccountTab";
  private scopeTab = "scopeTab";
  private assignEmptyRoleBtn = (type: string) =>
    `no-roles-for-this-${type}-empty-action`;
  private assignRoleBtn = "assignRole";
  private unAssignBtn = "unAssignRole";
  private unAssignDrpDwnBtn = '.pf-c-table__action li button[role="menuitem"]';
  private assignBtn = "assign";
  private hideInheritedRolesBtn = "#hideInheritedRoles";
  private assignedRolesTable = "assigned-roles";
  private namesColumn = 'td[data-label="Name"]:visible';
  private actionBtn = 'button[aria-label="Actions"]';

  constructor(type: string) {
    this.type = type;
  }

  goToServiceAccountTab() {
    cy.findByTestId(this.tab).click();
    return this;
  }

  goToScopeTab() {
    cy.findByTestId(this.scopeTab).click();
    return this;
  }

  assignRole(notEmpty = true) {
    cy.findByTestId(
      notEmpty ? this.assignEmptyRoleBtn(this.type) : this.assignRoleBtn
    ).click();
    return this;
  }

  assign() {
    cy.findByTestId(this.assignBtn).click();
    return this;
  }

  unAssign() {
    cy.findByTestId(this.unAssignBtn).click();
    return this;
  }

  unAssignFromDropdown() {
    cy.get(this.unAssignDrpDwnBtn).click();
    return this;
  }

  hideInheritedRoles() {
    cy.get(this.hideInheritedRolesBtn).check();
    return this;
  }

  unhideInheritedRoles() {
    cy.get(this.hideInheritedRolesBtn).uncheck({ force: true });
    return this;
  }

  selectRow(name: string, modal = false) {
    cy.get(modal ? ".pf-c-modal-box " : "" + this.namesColumn)
      .contains(name)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    return this;
  }

  checkRoles(roleNames: string[], exist = true) {
    if (roleNames.length) {
      cy.findByTestId(this.assignedRolesTable)
        .get(this.namesColumn)
        .should((roles) => {
          for (let index = 0; index < roleNames.length; index++) {
            const roleName = roleNames[index];

            if (exist) {
              expect(roles).to.contain(roleName);
            } else {
              expect(roles).not.to.contain(roleName);
            }
          }
        });
    } else {
      cy.findByTestId(this.assignedRolesTable).should("not.exist");
    }
    return this;
  }
}
