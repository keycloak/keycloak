import { v4 as uuid } from "uuid";
import Form from "../support/forms/Form";
import FormValidation from "../support/forms/FormValidation";
import Select from "../support/forms/Select";
import CIBAPolicyPage from "../support/pages/admin-ui/manage/authentication/CIBAPolicyPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();

describe("Authentication - Policies - CIBA", () => {
  const realmName = uuid();

  before(() => adminClient.createRealm(realmName));

  after(() => adminClient.deleteRealm(realmName));

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
    sidebarPage.goToAuthentication();
    CIBAPolicyPage.goToTab();
  });

  it("displays the initial state", () => {
    Select.assertSelectedItem(
      CIBAPolicyPage.getBackchannelTokenDeliveryModeSelect(),
      "Poll",
    );
    CIBAPolicyPage.getExpiresInput().should("have.value", "120");
    CIBAPolicyPage.getIntervalInput().should("have.value", "5");
    Form.assertSaveButtonDisabled();
  });

  it("validates the fields", () => {
    // Required fields.
    CIBAPolicyPage.getExpiresInput().clear();
    CIBAPolicyPage.getIntervalInput().clear();

    FormValidation.assertRequired(CIBAPolicyPage.getExpiresInput());
    FormValidation.assertRequired(CIBAPolicyPage.getIntervalInput());
    Form.assertSaveButtonDisabled();

    // Fields with minimum value.
    CIBAPolicyPage.getExpiresInput().type("9");
    CIBAPolicyPage.getIntervalInput().type("-1");

    FormValidation.assertMinValue(CIBAPolicyPage.getExpiresInput(), 10);
    FormValidation.assertMinValue(CIBAPolicyPage.getIntervalInput(), 0);
    Form.assertSaveButtonDisabled();

    // Fields with maximum value.
    CIBAPolicyPage.getExpiresInput().clear().type("601");
    CIBAPolicyPage.getIntervalInput().clear().type("601");

    FormValidation.assertMaxValue(CIBAPolicyPage.getExpiresInput(), 600);
    FormValidation.assertMaxValue(CIBAPolicyPage.getIntervalInput(), 600);
    Form.assertSaveButtonDisabled();
  });

  it("saves the form", () => {
    // Select new values for fields.
    Select.selectItem(
      CIBAPolicyPage.getBackchannelTokenDeliveryModeSelect(),
      "Ping",
    );
    CIBAPolicyPage.getExpiresInput().clear().type("140");
    CIBAPolicyPage.getIntervalInput().clear().type("20");

    // Save form.
    Form.clickSaveButton();
    CIBAPolicyPage.assertSaveSuccess();

    // Assert values are saved.
    Select.assertSelectedItem(
      CIBAPolicyPage.getBackchannelTokenDeliveryModeSelect(),
      "Ping",
    );
    CIBAPolicyPage.getExpiresInput().should("have.value", "140");
    CIBAPolicyPage.getIntervalInput().should("have.value", "20");
  });
});
