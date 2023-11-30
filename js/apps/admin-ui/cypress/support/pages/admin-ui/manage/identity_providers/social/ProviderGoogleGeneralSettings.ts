import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const hosted_domain_input_test_value = "hosted_domain_input_test_value";

export default class ProviderGoogleGeneralSettings extends ProviderBaseGeneralSettingsPage {
  #hostedDomainInput = "hostedDomain";
  #useUserIpParamSwitch = "userIp";
  #requestRefreshTokenSwitch = "offlineAccess";

  public typeHostedDomainInput(value: string) {
    cy.findByTestId(this.#hostedDomainInput).type(value);
    cy.findByTestId(this.#hostedDomainInput).blur();
    return this;
  }

  public clickUseUserIpParamSwitch() {
    cy.findByTestId(this.#useUserIpParamSwitch).parent().click();
    return this;
  }

  public clickRequestRefreshTokenSwitch() {
    cy.findByTestId(this.#requestRefreshTokenSwitch).parent().click();
    return this;
  }

  public assertHostedDomainInputEqual(value: string) {
    cy.findByTestId(this.#hostedDomainInput).should("have.value", value);
    return this;
  }

  public assertUseUserIpParamSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.findByTestId(this.#useUserIpParamSwitch),
      isOn,
    );
    return this;
  }

  public assertRequestRefreshTokenSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.findByTestId(this.#requestRefreshTokenSwitch),
      isOn,
    );
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
    this.assertSwitchStateOn(cy.findByTestId(this.#useUserIpParamSwitch));
    this.assertSwitchStateOn(cy.findByTestId(this.#requestRefreshTokenSwitch));
    return this;
  }
}
