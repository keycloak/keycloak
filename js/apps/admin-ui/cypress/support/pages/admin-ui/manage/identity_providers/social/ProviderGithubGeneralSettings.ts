import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const base_url_input_test_value = "base_url_input_test_value";
const api_url_input_test_value = "api_url_input_test_value";

export default class ProviderGithubGeneralSettings extends ProviderBaseGeneralSettingsPage {
  #baseUrlInput = "baseUrl";
  #apiUrlInput = "apiUrl";

  public typeBaseUrlInput(value: string) {
    cy.findByTestId(this.#baseUrlInput).type(value);
    cy.findByTestId(this.#baseUrlInput).blur();
    return this;
  }

  public typeApiUrlInput(value: string) {
    cy.findByTestId(this.#apiUrlInput).type(value);
    cy.findByTestId(this.#apiUrlInput).blur();
    return this;
  }

  public assertBaseUrlInputInputEqual(value: string) {
    cy.findByTestId(this.#baseUrlInput).should("have.value", value);
    return this;
  }

  public assertApiUrlInputEqual(value: string) {
    cy.findByTestId(this.#apiUrlInput).should("have.value", value);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeBaseUrlInput(idpName + base_url_input_test_value);
    this.typeApiUrlInput(idpName + api_url_input_test_value);
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertBaseUrlInputInputEqual(idpName + base_url_input_test_value);
    this.assertApiUrlInputEqual(idpName + api_url_input_test_value);
    return this;
  }
}
