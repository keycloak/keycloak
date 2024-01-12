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
    return cy.findAllByTestId("attributes.cibaExpiresIn");
  }

  static getIntervalInput() {
    return cy.findAllByTestId("attributes.cibaInterval");
  }

  static assertSaveSuccess() {
    masthead.checkNotificationMessage("CIBA policy successfully updated");
  }
}
