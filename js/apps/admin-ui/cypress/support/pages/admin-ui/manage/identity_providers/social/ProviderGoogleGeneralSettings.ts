import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const hosted_domain_input_test_value = "hosted_domain_input_test_value";

export default class ProviderGoogleGeneralSettings extends ProviderBaseGeneralSettingsPage {
  private hostedDomainInput = "#googleHostedDomain";
  private useUserIpParamSwitch = "#googleUserIp";
  private requestRefreshTokenSwitch = "#googleOfflineAccess";

  public typeHostedDomainInput(value: string) {
    cy.get(this.hostedDomainInput).type(value).blur();
    return this;
  }

  public clickUseUserIpParamSwitch() {
    cy.get(this.useUserIpParamSwitch).parent().click();
    return this;
  }

  public clickRequestRefreshTokenSwitch() {
    cy.get(this.requestRefreshTokenSwitch).parent().click();
    return this;
  }

  public assertHostedDomainInputEqual(value: string) {
    cy.get(this.hostedDomainInput).should("have.value", value);
    return this;
  }

  public assertUseUserIpParamSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.useUserIpParamSwitch), isOn);
    return this;
  }

  public assertRequestRefreshTokenSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.requestRefreshTokenSwitch), isOn);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeHostedDomainInput(hosted_domain_input_test_value);
    this.clickUseUserIpParamSwitch();
    this.clickRequestRefreshTokenSwitch();
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertHostedDomainInputEqual(hosted_domain_input_test_value);
    this.assertSwitchStateOn(cy.get(this.useUserIpParamSwitch));
    this.assertSwitchStateOn(cy.get(this.requestRefreshTokenSwitch));
    return this;
  }
}
