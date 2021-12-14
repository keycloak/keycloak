export default class AddMapperPage {
  private mappersTab = "mappers-tab";
  private noMappersAddMapperButton = "no-mappers-empty-action";
  private idpMapperSelectToggle = "#identityProviderMapper";
  private idpMapperSelect = "idp-mapper-select";
  private addMapperButton = "#add-mapper-button";

  private mapperNameInput = "#kc-name";
  private mapperRoleInput = "mapper-role-input";
  private attribute = "user.attribute";
  private attributeName = "attribute.name";
  private attributeFriendlyName = "attribute.friendly.name";
  private attributeValue = "attribute-value";
  private claimInput = "claim";
  private claimValueInput = "claim-value-input";
  private socialProfileJSONfieldPath = "jsonField";
  private userAttribute = "attribute";
  private userAttributeName = "userAttribute";
  private userAttributeValue = "attribute.value";
  private userSessionAttribute = "attribute";
  private userSessionAttributeValue = "attribute.value";
  private newMapperSaveButton = "new-mapper-save-button";
  private regexAttributeValuesSwitch = "are.attribute.values.regex";
  private syncmodeSelectToggle = "#syncMode";
  private attributesKeyInput = 'input[name="config.attributes[0].key"]';
  private attributesValueInput = 'input[name="config.attributes[0].value"]';
  private template = "template";
  private target = "#target";
  private targetDropdown = "#target-dropdown";

  goToMappersTab() {
    cy.findByTestId(this.mappersTab).click();
    return this;
  }

  emptyStateAddMapper() {
    cy.findByTestId(this.noMappersAddMapperButton).click();
    return this;
  }

  addMapper() {
    cy.get(this.addMapperButton).click();
    return this;
  }

  clickCreateDropdown() {
    cy.contains("Add provider").click();
    return this;
  }

  saveNewMapper() {
    cy.findByTestId(this.newMapperSaveButton).click();
    return this;
  }

  toggleSwitch(switchName: string) {
    cy.findByTestId(switchName).click({ force: true });

    return this;
  }

  fillSocialMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("legacy").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.socialProfileJSONfieldPath).clear();
    cy.findByTestId(this.socialProfileJSONfieldPath).type(
      "social profile JSON field path"
    );

    cy.findByTestId(this.userAttributeName).clear();

    cy.findByTestId(this.userAttributeName).type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addRoleToMapperForm() {
    cy.get("#group-role-select-typeahead")
      .click()
      .get(".pf-c-select__menu-item")
      .first()
      .click();
    cy.get("#role-role-select-typeahead")
      .click()
      .get(".pf-c-select__menu-item")
      .first()
      .click();

    return this;
  }

  addAdvancedAttrToRoleMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Advanced Attribute to Role")
      .click();

    cy.get(this.attributesKeyInput).clear();
    cy.get(this.attributesKeyInput).type("key");

    cy.get(this.attributesValueInput).clear();
    cy.get(this.attributesValueInput).type("value");

    this.toggleSwitch(this.regexAttributeValuesSwitch);

    this.addRoleToMapperForm();

    this.saveNewMapper();

    return this;
  }

  addUsernameTemplateImporterMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Username Template Importer")
      .click();

    cy.findByTestId(this.template).clear();
    cy.findByTestId(this.template).type("Template");

    cy.get(this.target).click().parent().contains("BROKER_ID").click();

    this.saveNewMapper();

    return this;
  }

  addHardcodedUserSessionAttrMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Hardcoded User Session Attribute")
      .click();

    cy.findByTestId(this.userSessionAttribute).clear();
    cy.findByTestId(this.userSessionAttribute).type("user session attribute");

    cy.findByTestId(this.userSessionAttributeValue).clear();
    cy.findByTestId(this.userSessionAttributeValue).type(
      "user session attribute value"
    );

    this.saveNewMapper();

    return this;
  }

  addSAMLAttrImporterMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.attributeName).clear();
    cy.findByTestId(this.attributeName).type("attribute name");

    cy.findByTestId(this.attributeFriendlyName).clear();
    cy.findByTestId(this.attributeFriendlyName).type("attribute friendly name");

    cy.findByTestId(this.attribute).clear().type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addOIDCAttrImporterMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.claimInput).clear().type("claim");
    cy.findByTestId(this.attribute).clear().type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addHardcodedRoleMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect).contains("Hardcoded Role").click();

    this.addRoleToMapperForm();
    this.saveNewMapper();

    return this;
  }

  addHardcodedAttrMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Hardcoded Attribute")
      .click();

    cy.findByTestId(this.userAttribute).clear().type("user session attribute");

    cy.findByTestId(this.userAttributeValue)
      .clear()
      .type("user session attribute value");

    this.saveNewMapper();

    return this;
  }

  addSAMLAttributeToRoleMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("SAML Attribute to Role")
      .click();

    this.addRoleToMapperForm();

    this.saveNewMapper();

    return this;
  }

  editUsernameTemplateImporterMapper() {
    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("legacy").click();

    cy.findByTestId(this.template).type("_edited");

    cy.get(this.target).click().parent().contains("BROKER_USERNAME").click();

    this.saveNewMapper();

    return this;
  }

  editSocialMapper() {
    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.findByTestId(this.socialProfileJSONfieldPath).clear();

    cy.findByTestId(this.socialProfileJSONfieldPath).type(
      "social profile JSON field path edited"
    );

    cy.findByTestId(this.userAttributeName).clear();

    cy.findByTestId(this.userAttributeName).type("user attribute name edited");

    this.saveNewMapper();

    return this;
  }

  editSAMLorOIDCMapper() {
    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("legacy").click();

    cy.get(this.attributesKeyInput).clear();
    cy.get(this.attributesKeyInput).type("key_edited");

    cy.get(this.attributesValueInput).clear();
    cy.get(this.attributesValueInput).type("value_edited");

    this.toggleSwitch(this.regexAttributeValuesSwitch);

    this.saveNewMapper();

    return this;
  }

  addOIDCAttributeImporterMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.claimInput).clear();
    cy.findByTestId(this.claimInput).type("claim");

    cy.findByTestId(this.userAttributeName).clear();
    cy.findByTestId(this.userAttributeName).type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addOIDCClaimToRoleMapper(name: string) {
    cy.get(this.mapperNameInput).clear();

    cy.get(this.mapperNameInput).clear().type(name);

    cy.get(this.syncmodeSelectToggle).click();

    cy.findByTestId("inherit").click();

    cy.get(this.idpMapperSelectToggle).click();

    cy.findByTestId(this.idpMapperSelect).contains("Claim to Role").click();

    cy.findByTestId("attribute-key-input").clear().type("key");
    cy.findByTestId("attribute-value-input").clear().type("value");

    this.toggleSwitch("are.claim.values.regex");

    this.addRoleToMapperForm();
    this.saveNewMapper();

    return this;
  }
}
