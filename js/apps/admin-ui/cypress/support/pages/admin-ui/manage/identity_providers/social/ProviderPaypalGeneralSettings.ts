import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

export default class ProviderPaypalGeneralSettings extends ProviderBaseGeneralSettingsPage {
  #targetSandboxSwitch = "sandbox";

  public clickTargetSandboxSwitch() {
    cy.findByTestId(this.#targetSandboxSwitch).parent().click();
    return this;
  }

  public assertTargetSandboxSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.findByTestId(this.#targetSandboxSwitch), isOn);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.clickTargetSandboxSwitch();
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertSwitchStateOn(cy.findByTestId(this.#targetSandboxSwitch));
    return this;
  }
}
