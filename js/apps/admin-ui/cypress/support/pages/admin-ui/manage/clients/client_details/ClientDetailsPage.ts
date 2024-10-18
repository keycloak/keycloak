import CommonPage from "../../../../CommonPage";
import AdvancedTab from "./tabs/AdvancedTab";
import AuthorizationTab from "./tabs/AuthorizationTab";
import ClientScopesTab from "./tabs/ClientScopesTab";
import CredentialsTab from "./tabs/CredentialsTab";
import KeysTab from "./tabs/KeysTab";
import RolesTab from "./tabs/RolesTab";
import SettingsTab from "./tabs/SettingsTab";

export enum ClientsDetailsTab {
  Settings = "Settings",
  Keys = "Keys",
  Credentials = "Credentials",
  Roles = "Roles",
  Sessions = "Sessions",
  Permissions = "Permissions",
  ClientScopes = "Client scopes",
  Authorization = "Authorization",
  ServiceAccountsRoles = "Service accounts roles",
  Advanced = "Advanced",
  Scope = "Scope",
  UserEvents = "User events",
}

export default class ClientDetailsPage extends CommonPage {
  #settingsTab = new SettingsTab();
  #keysTab = new KeysTab();
  #credentialsTab = new CredentialsTab();
  #rolesTab = new RolesTab();
  #clientScopesTab = new ClientScopesTab();
  #authorizationTab = new AuthorizationTab();
  #advancedTab = new AdvancedTab();
  #clientScopesSetupTab = "clientScopesSetupTab";
  #clientScopesEvaluateTab = "clientScopesEvaluateTab";
  #evaluateEffectiveProtocolMappersTab = "effective-protocol-mappers-tab";
  #evaluateEffectiveRoleScopeMappingsTab = "effective-role-scope-mappings-tab";
  #evaluateGeneratedAccessTokenTab = "generated-access-token-tab";
  #evaluateGeneratedIdTokenTab = "generated-id-token-tab";
  #evaluateGeneratedUserInfoTab = "generated-user-info-tab";

  goToSettingsTab() {
    this.tabUtils().clickTab(ClientsDetailsTab.Settings);
    return this.#settingsTab;
  }

  goToKeysTab() {
    this.tabUtils().clickTab(ClientsDetailsTab.Keys);
    return this.#keysTab;
  }

  goToCredentials() {
    this.tabUtils().clickTab(ClientsDetailsTab.Credentials);
    return this.#credentialsTab;
  }

  goToRolesTab() {
    this.tabUtils().clickTab(ClientsDetailsTab.Roles);
    return this.#rolesTab;
  }

  goToClientScopesTab() {
    this.tabUtils().clickTab(ClientsDetailsTab.ClientScopes);
    return this.#clientScopesTab;
  }

  goToAuthorizationTab() {
    this.tabUtils().clickTab(ClientsDetailsTab.Authorization);
    return this.#authorizationTab;
  }

  goToAdvancedTab() {
    this.tabUtils().clickTab(ClientsDetailsTab.Advanced);
    return this.#advancedTab;
  }

  goToClientScopesSetupTab() {
    cy.findByTestId(this.#clientScopesSetupTab).click();
    return this;
  }

  goToClientScopesEvaluateTab() {
    this.goToClientScopesTab();
    cy.findByTestId(this.#clientScopesEvaluateTab).click();
    return this;
  }

  goToClientScopesEvaluateEffectiveProtocolMappersTab() {
    cy.findByTestId(this.#evaluateEffectiveProtocolMappersTab).click();
    return this;
  }

  goToClientScopesEvaluateEffectiveRoleScopeMappingsTab() {
    cy.findByTestId(this.#evaluateEffectiveRoleScopeMappingsTab).click();
    return this;
  }

  goToClientScopesEvaluateGeneratedAccessTokenTab() {
    cy.findByTestId(this.#evaluateGeneratedAccessTokenTab).click();
    return this;
  }

  goToClientScopesEvaluateGeneratedIdTokenTab() {
    cy.findByTestId(this.#evaluateGeneratedIdTokenTab).click();
    return this;
  }

  goToClientScopesEvaluateGeneratedUserInfoTab() {
    cy.findByTestId(this.#evaluateGeneratedUserInfoTab).click();
    return this;
  }
}
