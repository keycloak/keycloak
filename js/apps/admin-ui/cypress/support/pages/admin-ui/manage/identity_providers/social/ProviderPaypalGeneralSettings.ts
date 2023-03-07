import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

export default class ProviderPaypalGeneralSettings extends ProviderBaseGeneralSettingsPage {
  private targetSandboxSwitch = "#paypalSandbox";

  public clickTargetSandboxSwitch() {
    cy.get(this.targetSandboxSwitch).parent().click();
    return this;
  }

  public assertTargetSandboxSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.targetSandboxSwitch), isOn);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.clickTargetSandboxSwitch();
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertSwitchStateOn(cy.get(this.targetSandboxSwitch));
    return this;
  }
}
