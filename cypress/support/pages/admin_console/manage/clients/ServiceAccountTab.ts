const expect = chai.expect;
export default class ServiceAccountTab {
  private tab = "#pf-tab-serviceAccount-serviceAccount";
  private assignedRolesTable = "assigned-roles";
  private namesColumn = 'td[data-label="Name"]:visible';

  goToTab() {
    cy.get(this.tab).click();
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
