import ModalUtils from "../../../../../../util/ModalUtils";
import PageObject from "../../../../components/PageObject";
import Masthead from "../../../../Masthead";

const masthead = new Masthead();
const modal = new ModalUtils();

export default class AdminEventsSettingsTab extends PageObject {
  private saveEventsSwitch = "#adminEventsEnabled-switch";
  private clearAdminEventsBtn = "#clear-admin-events";
  private saveBtn = "#save-admin";

  clearAdminEvents() {
    cy.get(this.clearAdminEventsBtn).click();
    modal.checkModalTitle("Clear events");
    modal.confirmModal();
    masthead.checkNotificationMessage("The admin events have been cleared");
    return this;
  }

  disableSaveEvents() {
    super.assertSwitchStateOn(cy.get(this.saveEventsSwitch));
    cy.get(this.saveEventsSwitch).parent().click();
    modal.checkModalTitle("Unsave events?");
    modal.confirmModal();
    super.assertSwitchStateOff(cy.get(this.saveEventsSwitch));
    return this;
  }

  enableSaveEvents() {
    super.assertSwitchStateOff(cy.get(this.saveEventsSwitch));
    cy.get(this.saveEventsSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.saveEventsSwitch));
    return this;
  }

  save() {
    cy.get(this.saveBtn).click();
    return this;
  }
}
