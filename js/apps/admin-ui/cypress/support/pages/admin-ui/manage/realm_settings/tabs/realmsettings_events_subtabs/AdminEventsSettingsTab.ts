import ModalUtils from "../../../../../../util/ModalUtils";
import PageObject from "../../../../components/PageObject";
import Masthead from "../../../../Masthead";

const masthead = new Masthead();
const modal = new ModalUtils();

export default class AdminEventsSettingsTab extends PageObject {
  #saveEventsSwitch = "#adminEventsEnabled-switch";
  #clearAdminEventsBtn = "#clear-admin-events";
  #saveBtn = "#save-admin";

  clearAdminEvents() {
    cy.get(this.#clearAdminEventsBtn).click();
    modal.checkModalTitle("Clear events");
    cy.intercept("/admin/realms/*/admin-events").as("clearEvents");
    modal.confirmModal();
    cy.wait("@clearEvents");
    masthead.checkNotificationMessage("The admin events have been cleared");
    return this;
  }

  disableSaveEvents() {
    super.assertSwitchStateOn(cy.get(this.#saveEventsSwitch));
    cy.get(this.#saveEventsSwitch).parent().click();
    modal.checkModalTitle("Unsave events?");
    modal.confirmModal();
    super.assertSwitchStateOff(cy.get(this.#saveEventsSwitch));
    return this;
  }

  enableSaveEvents() {
    super.assertSwitchStateOff(cy.get(this.#saveEventsSwitch));
    cy.get(this.#saveEventsSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#saveEventsSwitch));
    return this;
  }

  save(
    { waitForRealm, waitForConfig } = {
      waitForRealm: true,
      waitForConfig: false,
    },
  ) {
    waitForRealm && cy.intercept("/admin/realms/*").as("saveRealm");
    waitForConfig &&
      cy.intercept("/admin/realms/*/events/config").as("saveConfig");

    cy.get(this.#saveBtn).click();

    waitForRealm && cy.wait("@saveRealm");
    waitForConfig && cy.wait("@saveConfig");

    return this;
  }
}
