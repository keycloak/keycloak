import Masthead from "../../Masthead";

const masthead = new Masthead();

export default class CIBAPolicyPage {
  static goToTab() {
    cy.findByTestId("policies").click();
    cy.findByTestId("tab-ciba-policy").click();
    return this;
  }

  static getBackchannelTokenDeliveryModeSelect() {
    return cy.get("#cibaBackchannelTokenDeliveryMode");
  }

  static getExpiresInput() {
    return cy.get("#cibaExpiresIn");
  }

  static getIntervalInput() {
    return cy.get("#cibaInterval");
  }

  static assertSaveSuccess() {
    masthead.checkNotificationMessage("CIBA policy successfully updated");
  }
}
