import Select from "../../../../forms/Select";
import CommonPage from "../../../CommonPage";

export default class IdentityProviderTab extends CommonPage {
  goToTab() {
    cy.findByTestId("identityProvidersTab").click();
  }

  fillForm(data: { name: string; domain: string; public: boolean }) {
    Select.selectItem(cy.findByTestId("alias"), data.name);
    Select.selectItem(cy.get("#kc🍺org🍺domain"), data.domain);
    if (data.public) {
      cy.findByAltText("config.kc🍺org🍺broker🍺public").click();
    }
  }

  assertAddedSuccess() {
    this.masthead().checkNotificationMessage(
      "Identity provider successfully linked to organization",
    );
  }
}
