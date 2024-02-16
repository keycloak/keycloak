import ModalUtils from "../../../../../../util/ModalUtils";
import PageObject from "../../../../components/PageObject";
import Masthead from "../../../../Masthead";

const masthead = new Masthead();
const modal = new ModalUtils();

export default class UserEventsSettingsTab extends PageObject {
  #saveEventsSwitch = "#eventsEnabled-switch";
  #clearUserEventsBtn = "#clear-user-events";
  #saveBtn = "#save-user";

  clearUserEvents() {
    cy.get(this.#clearUserEventsBtn).click();
    modal.checkModalTitle("Clear events");
    modal.confirmModal();
    masthead.checkNotificationMessage("The user events have been cleared");
    return this;
  }

  disableSaveEventsSwitch() {
    cy.get(this.#saveEventsSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#saveEventsSwitch));
    this.waitForPageLoad();
    modal.checkModalTitle("Unsave events?");
    modal.confirmModal();
    super.assertSwitchStateOff(cy.get(this.#saveEventsSwitch));
    return this;
  }

  enableSaveEventsSwitch() {
    cy.get(this.#saveEventsSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#saveEventsSwitch));
    return this;
  }

  save() {
    cy.get(this.#saveBtn).click();
    return this;
  }
}
