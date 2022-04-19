import CommonPage from "../../../../CommonPage";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";

export default class CreateAuthorizationScopePage extends CommonPage {
  fillScopeForm(scope: ScopeRepresentation) {
    Object.entries(scope).map(([key, value]) => cy.get(`#${key}`).type(value));
    return this;
  }
}
