import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";

type PermissionType = "resource" | "scope";

export default class AuthorizationTab {
  private tabName = "#pf-tab-authorization-authorization";
  private resourcesTabName = "#pf-tab-41-resources";
  private scopeTabName = "#pf-tab-42-scopes";
  private policyTabName = "#pf-tab-43-policies";
  private permissionsTabName = "#pf-tab-44-permissions";
  private nameColumnPrefix = "name-column-";
  private emptyPolicyCreateButton = "no-policies-empty-action";
  private createPolicyButton = "createPolicy";
  private createResourceButton = "createResource";
  private createScopeButton = "no-authorization-scopes-empty-action";
  private createPermissionDropdown = "permissionCreateDropdown";
  private permissionResourceDropdown = "#resources";

  goToAuthenticationTab() {
    cy.get(this.tabName).click();
    return this;
  }

  goToResourceSubTab() {
    cy.get(this.resourcesTabName).click();
    return this;
  }

  goToScopeSubTab() {
    cy.get(this.scopeTabName).click();
    return this;
  }

  goToPolicySubTab() {
    cy.get(this.policyTabName).click();
    return this;
  }

  goToPermissionsSubTab() {
    cy.get(this.permissionsTabName).click();
    return this;
  }

  goToCreateResource() {
    cy.findByTestId(this.createResourceButton).click();
    return this;
  }

  goToCreateScope() {
    cy.findByTestId(this.createScopeButton).click();
    return this;
  }

  goToCreatePolicy(type: string, first: boolean | undefined = false) {
    if (first) {
      cy.findByTestId(this.emptyPolicyCreateButton).click();
    } else {
      cy.findByTestId(this.createPolicyButton).click();
    }
    cy.findByTestId(type).click();
    return this;
  }

  goToCreatePermission(type: PermissionType) {
    cy.findByTestId(this.createPermissionDropdown).click();
    cy.findByTestId(`create-${type}`).click();
    return this;
  }

  fillBasePolicyForm(policy: { [key: string]: string }) {
    Object.entries(policy).map(([key, value]) =>
      cy.findByTestId(key).type(value)
    );
    return this;
  }

  inputClient(clientName: string) {
    cy.get("#clients").click();
    cy.get("ul li").contains(clientName).click();
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

  fillScopeForm(scope: ScopeRepresentation) {
    Object.entries(scope).map(([key, value]) => cy.get(`#${key}`).type(value));
    return this;
  }

  fillPermissionForm(permission: PolicyRepresentation) {
    Object.entries(permission).map(([key, value]) =>
      cy.get(`#${key}`).type(value)
    );
    return this;
  }

  selectResource(name: string) {
    cy.get(this.permissionResourceDropdown)
      .click()
      .parent()
      .parent()
      .findByText(name)
      .click();
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

  cancel() {
    cy.findByTestId("cancel").click();
    return this;
  }

  saveSettings() {
    cy.findByTestId("authenticationSettingsSave").click();
    return this;
  }

  pressCancel() {
    cy.findByTestId("cancel").click();
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
