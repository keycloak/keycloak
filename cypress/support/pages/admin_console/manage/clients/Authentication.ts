import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";

export default class AuthenticationTab {
  private tabName = "#pf-tab-authorization-authorization";
  private resourcesTabName = "#pf-tab-41-resources";
  private nameColumnPrefix = "name-column-";
  private createResourceButton = "createResource";

  goToTab() {
    cy.get(this.tabName).click();
    return this;
  }

  goToResourceSubTab() {
    cy.get(this.resourcesTabName).click();
    return this;
  }

  goToCreateResource() {
    cy.findAllByTestId(this.createResourceButton).click();
    return this;
  }

  fillResourceForm(resource: ResourceRepresentation) {
    Object.entries(resource).map(([key, value]) => {
      if (Array.isArray(value)) {
        for (let index = 0; index < value.length; index++) {
          const v = value[index];
          cy.get(`input[name="${key}[${index}].value"]`).type(v);
          cy.findByTestId("addValue").click();
        }
      } else {
        cy.get(`#${key}`).type(value);
      }
    });
    return this;
  }

  setPolicy(policyName: string) {
    cy.findByTestId(policyName).click();
    return this;
  }

  save() {
    cy.findByTestId("save").click();
    return this;
  }

  saveSettings() {
    cy.findByTestId("authenticationSettingsSave").click();
    return this;
  }

  pressCancel() {
    cy.findAllByTestId("cancel").click();
    return this;
  }

  private getResourceLink(name: string) {
    return cy.findByTestId(this.nameColumnPrefix + name);
  }

  goToResourceDetails(name: string) {
    this.getResourceLink(name).click();
    return this;
  }

  assertDefaultResource() {
    return this.assertResource("Default Resource");
  }

  assertResource(name: string) {
    this.getResourceLink(name).should("exist");
    return this;
  }
}
