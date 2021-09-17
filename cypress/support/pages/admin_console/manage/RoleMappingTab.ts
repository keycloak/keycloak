const expect = chai.expect;
export default class RoleMappingTab {
  private tab = "#pf-tab-serviceAccount-serviceAccount";
  private scopeTab = "scopeTab";
  private assignEmptyRole = "no-roles-for-this-client-empty-action";
  private assignRole = "assignRole";
  private unAssign = "unAssignRole";
  private assign = "assign";
  private hide = "#hideInheritedRoles";
  private assignedRolesTable = "assigned-roles";
  private namesColumn = 'td[data-label="Name"]:visible';

  goToServiceAccountTab() {
    cy.get(this.tab).click();
    return this;
  }

  goToScopeTab() {
    cy.findByTestId(this.scopeTab).click();
    return this;
  }

  clickAssignRole(notEmpty = true) {
    cy.findByTestId(notEmpty ? this.assignEmptyRole : this.assignRole).click();
    return this;
  }

  clickAssign() {
    cy.findByTestId(this.assign).click();
    return this;
  }

  clickUnAssign() {
    cy.findByTestId(this.unAssign).click();
    return this;
  }

  hideInheritedRoles() {
    cy.get(this.hide).check();
    return this;
  }

  selectRow(name: string) {
    cy.get(this.namesColumn)
      .contains(name)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    return this;
  }

  checkRoles(roleNames: string[]) {
    if (roleNames.length) {
      cy.findByTestId(this.assignedRolesTable)
        .get(this.namesColumn)
        .should((roles) => {
          for (let index = 0; index < roleNames.length; index++) {
            const roleName = roleNames[index];
            expect(roles).to.contain(roleName);
          }
        });
    } else {
      cy.findByTestId(this.assignedRolesTable).should("not.exist");
    }
    return this;
  }
}
