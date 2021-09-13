export default class AddMapperPage {
  private mappersTab = "mappers-tab";
  private noMappersAddMapperButton = "no-mappers-empty-action";
  private idpMapperSelectToggle = "#identityProviderMapper";
  private idpMapperSelect = "idp-mapper-select";

  private mapperNameInput = "#kc-name";
  private mapperRoleInput = "mapper-role-input";
  private userSessionAttribute = "user-session-attribute";
  private userSessionAttributeValue = "user-session-attribute-value";
  private newMapperSaveButton = "new-mapper-save-button";
  private regexAttributeValuesSwitch = "regex-attribute-values-switch";
  private syncmodeSelectToggle = "#syncMode";
  private attributesKeyInput = 'input[name="config.attributes[0].key"]';
  private attributesValueInput = 'input[name="config.attributes[0].value"]';
  private selectRoleButton = "select-role-button";
  private radio = "[type=radio]";
  private addAssociatedRolesModalButton = "add-associated-roles-button";

  goToMappersTab() {
    cy.getId(this.mappersTab).click();
    return this;
  }

  clickAdd() {
    cy.getId(this.noMappersAddMapperButton).click();
    return this;
  }

  clickCreateDropdown() {
    cy.contains("Add provider").click();
    return this;
  }

  saveNewMapper() {
    cy.getId(this.newMapperSaveButton).click();
    return this;
  }

  toggleSwitch(switchName: string) {
    cy.getId(switchName).click({ force: true });

    return this;
  }

  fillSocialMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.getId("legacy").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.getId(this.idpMapperSelect).contains("Attribute Importer").click();

    cy.getId(this.userSessionAttribute).clear();
    cy.getId(this.userSessionAttribute).type("user session attribute");
    cy.getId(this.userSessionAttributeValue).clear();

    cy.getId(this.userSessionAttributeValue).type(
      "user session attribute value"
    );

    return this;
  }

  addRoleToMapperForm() {
    const load = "/auth/admin/realms/master/roles";
    cy.intercept(load).as("load");

    cy.get(this.radio).eq(0).check();

    cy.getId(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.getId(this.mapperRoleInput).should("have.value", "admin");

    return this;
  }

  fillSAMLorOIDCMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.getId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.getId(this.idpMapperSelect)
      .contains("Hardcoded User Session Attribute")
      .click();

    cy.get(this.attributesKeyInput).clear();
    cy.get(this.attributesKeyInput).type("key");

    cy.get(this.attributesValueInput).clear();
    cy.get(this.attributesValueInput).type("value");

    this.toggleSwitch(this.regexAttributeValuesSwitch);

    cy.getId(this.selectRoleButton).click();

    this.addRoleToMapperForm();

    this.saveNewMapper();

    return this;
  }
}
