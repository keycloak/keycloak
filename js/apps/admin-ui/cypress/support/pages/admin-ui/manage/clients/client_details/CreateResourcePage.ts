import CommonPage from "../../../../CommonPage";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";

export default class CreateResourcePage extends CommonPage {
  fillResourceForm(resource: ResourceRepresentation) {
    Object.entries(resource).map(([key, value]) => {
      if (Array.isArray(value)) {
        for (let index = 0; index < value.length; index++) {
          const v = value[index];
          cy.findByTestId(`${key}${index}`).type(v);
          cy.findByTestId("addValue").click();
        }
      } else {
        cy.get(`#${key}`).type(value);
      }
    });
    return this;
  }
}
