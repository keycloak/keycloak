import CommonPage from "../../../../CommonPage";
import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";

export default class CreatePermissionPage extends CommonPage {
  fillPermissionForm(permission: PolicyRepresentation) {
    Object.entries(permission).map(([key, value]) =>
      cy.get(`#${key}`).type(value),
    );
    return this;
  }
}
