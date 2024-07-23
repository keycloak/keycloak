import CommonPage from "../../../../../../CommonPage";

export enum ClaimJsonType {
  String = "String",
  Long = "long",
  Int = "int",
  Boolean = "boolean",
  Json = "JSON",
}

export default class MapperDetailsPage extends CommonPage {
  #userAttributeInput = '[data-testid="config.user🍺attribute"]';
  #tokenClaimNameInput = '[data-testid="claim.name"]';
  #claimJsonType = '[id="jsonType.label"]';

  fillUserAttribute(userAttribute: string) {
    cy.get(this.#userAttributeInput).clear().type(userAttribute);

    return this;
  }

  checkUserAttribute(userAttribute: string) {
    cy.get(this.#userAttributeInput).should("have.value", userAttribute);

    return this;
  }

  fillTokenClaimName(name: string) {
    cy.get(this.#tokenClaimNameInput).clear().type(name);

    return this;
  }

  checkTokenClaimName(name: string) {
    cy.get(this.#tokenClaimNameInput).should("have.value", name);

    return this;
  }

  changeClaimJsonType(type: string) {
    cy.get(this.#claimJsonType).click();
    cy.get(this.#claimJsonType).parent().contains(type).click();

    return this;
  }

  checkClaimJsonType(type: string) {
    cy.get(this.#claimJsonType).should("contain", type);

    return this;
  }
}
