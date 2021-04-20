const expect = chai.expect;
export default class RoleMappingTab {
  private tab = "#pf-tab-serviceAccount-serviceAccount";
  private scopeTab = "scopeTab";
  private assignRole = "assignRole";
  private assign = "assign";
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
    cy.getId(this.assignedRolesTable)
      .get(this.namesColumn)
      .should((roles) => {
        for (let index = 0; index < roleNames.length; index++) {
          const roleName = roleNames[index];
          expect(roles).to.contain(roleName);
        }
      });
    return this;
  }
}
