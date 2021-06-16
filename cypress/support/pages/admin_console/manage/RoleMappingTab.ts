const expect = chai.expect;
export default class RoleMappingTab {
  private tab = "#pf-tab-serviceAccount-serviceAccount";
  private scopeTab = "scopeTab";
  private assignRole = "no-roles-for-this-client-empty-action";
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
    cy.getId(this.scopeTab).click();
    return this;
  }

  clickAssignRole() {
    cy.getId(this.assignRole).click();
    return this;
  }

  clickAssign() {
    cy.getId(this.assign).click();
    return this;
  }

  clickUnAssign() {
    cy.getId(this.unAssign).click();
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
    if (roleNames && roleNames.length) {
      cy.getId(this.assignedRolesTable)
        .get(this.namesColumn)
        .should((roles) => {
          for (let index = 0; index < roleNames.length; index++) {
            const roleName = roleNames[index];
            expect(roles).to.contain(roleName);
          }
        });
    } else {
      cy.getId(this.assignedRolesTable).should("not.exist");
    }
    return this;
  }
}
