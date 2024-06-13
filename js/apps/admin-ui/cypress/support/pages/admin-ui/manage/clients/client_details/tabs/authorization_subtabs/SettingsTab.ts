import CommonPage from "../../../../../../CommonPage";

export default class SettingsTab extends CommonPage {
  saveSettings() {
    cy.findByTestId("authenticationSettingsSave").click();
    return this;
  }
}
