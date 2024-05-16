import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const TENANT_ID_VALUE = "12345678-9abc-def0-1234-56789abcdef0";

export default class ProviderMicrosoftGeneralSettings extends ProviderBaseGeneralSettingsPage {
  #tenantIdTestId = "tenantId";

  public typeTenantIdInput(value: string) {
    cy.findByTestId(this.#tenantIdTestId).type(value).blur();
    return this;
  }

  public assertTenantIdInputEqual(value: string) {
    cy.findByTestId(this.#tenantIdTestId).should("have.value", value);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeTenantIdInput(TENANT_ID_VALUE);
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertTenantIdInputEqual(TENANT_ID_VALUE);
    return this;
  }
}
