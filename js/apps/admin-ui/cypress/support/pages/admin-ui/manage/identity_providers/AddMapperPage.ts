import Select from "../../../../forms/Select";
import LegacyKeyValueInput from "../LegacyKeyValueInput";

export default class AddMapperPage {
  #mappersTab = "mappers-tab";
  #noMappersAddMapperButton = "no-mappers-empty-action";
  #idpMapperSelectToggle = "#identityProviderMapper";
  #idpMapperSelect = "idp-mapper-select";
  #addMapperButton = "#add-mapper-button";

  #mapperNameInput = "name";
  #attribute = "config.user🍺attribute";
  #attributeName = "attribute.name";
  #attributeFriendlyName = "attribute.friendly.name";
  #claimInput = "claim";
  #socialProfileJSONfieldPath = "jsonField";
  #userAttribute = "config.attribute";
  #userAttributeName = "config.userAttribute";
  #userAttributeValue = "attribute.value";
  #userSessionAttribute = "attribute";
  #userSessionAttributeValue = "attribute.value";
  #newMapperSaveButton = "new-mapper-save-button";
  #newMapperCancelButton = "new-mapper-cancel-button";
  #mappersUrl = "/oidc/mappers";
  #regexAttributeValuesSwitch = "are.attribute.values.regex";
  #syncmodeSelectToggle = "#syncMode";
  #attributesKeyInput = '[data-testid="config.attributes.0.key"]';
  #attributesValueInput = '[data-testid="config.attributes.0.value"]';
  #template = "template";
  #target = "#target";

  goToMappersTab() {
    cy.findByTestId(this.#mappersTab).click();
    return this;
  }

  emptyStateAddMapper() {
    cy.findByTestId(this.#noMappersAddMapperButton).click();
    return this;
  }

  addMapper() {
    cy.get(this.#addMapperButton).click();
    return this;
  }

  clickCreateDropdown() {
    cy.contains("Add provider").click();
    return this;
  }

  saveNewMapper() {
    cy.findByTestId(this.#newMapperSaveButton).click();
    return this;
  }

  cancelNewMapper() {
    cy.findByTestId(this.#newMapperCancelButton).click();
    return this;
  }

  toggleSwitch(switchName: string) {
    cy.findByTestId(switchName).click({ force: true });

    return this;
  }

  typeName(name: string) {
    cy.findByTestId(this.#mapperNameInput).clear();
    cy.findByTestId(this.#mapperNameInput).type(name);
  }

  fillSocialMapper(name: string) {
    this.typeName(name);

    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Legacy");
    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.#socialProfileJSONfieldPath).clear();
    cy.findByTestId(this.#socialProfileJSONfieldPath).type(
      "social profile JSON field path",
    );

    cy.findByTestId(this.#userAttributeName).clear();

    cy.findByTestId(this.#userAttributeName).type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addRoleToMapperForm() {
    cy.findByTestId("add-roles").click();
    cy.get("[aria-label='Select row 1']").click();
    cy.findByTestId("assign").click();
    return this;
  }

  addAdvancedAttrToRoleMapper(name: string) {
    this.typeName(name);
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Advanced Attribute to Role")
      .click();

    cy.findByTestId("attributes-add-row").click();
    cy.get(this.#attributesKeyInput).clear();
    cy.get(this.#attributesKeyInput).type("key");

    cy.get(this.#attributesValueInput).clear();
    cy.get(this.#attributesValueInput).type("value");

    this.toggleSwitch(this.#regexAttributeValuesSwitch);

    this.addRoleToMapperForm();

    this.saveNewMapper();

    return this;
  }

  addUsernameTemplateImporterMapper(name: string) {
    this.typeName(name);
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Username Template Importer")
      .click();

    cy.findByTestId(this.#template).clear();
    cy.findByTestId(this.#template).type("Template");

    cy.get(this.#target).click().parent().contains("BROKER_ID").click();

    this.saveNewMapper();

    return this;
  }

  addHardcodedUserSessionAttrMapper(name: string) {
    this.typeName(name);
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Hardcoded User Session Attribute")
      .click();

    cy.findByTestId(this.#userSessionAttribute).clear();
    cy.findByTestId(this.#userSessionAttribute).type("user session attribute");

    cy.findByTestId(this.#userSessionAttributeValue).clear();
    cy.findByTestId(this.#userSessionAttributeValue).type(
      "user session attribute value",
    );

    this.saveNewMapper();

    return this;
  }

  addSAMLAttrImporterMapper(name: string) {
    this.typeName(name);

    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.#attributeName).clear();
    cy.findByTestId(this.#attributeName).type("attribute name");

    cy.findByTestId(this.#attributeFriendlyName).clear();
    cy.findByTestId(this.#attributeFriendlyName).type(
      "attribute friendly name",
    );

    cy.findByTestId(this.#attribute).clear().type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addOIDCAttrImporterMapper(name: string) {
    this.typeName(name);

    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");
    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.#claimInput).clear().type("claim");
    cy.findByTestId(this.#attribute).clear().type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addHardcodedRoleMapper(name: string) {
    this.typeName(name);
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect).contains("Hardcoded Role").click();

    this.addRoleToMapperForm();
    this.saveNewMapper();

    return this;
  }

  addHardcodedAttrMapper(name: string) {
    this.typeName(name);
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Hardcoded Attribute")
      .click();

    cy.findByTestId(this.#userAttribute).clear().type("user session attribute");

    cy.findByTestId(this.#userAttributeValue)
      .clear()
      .type("user session attribute value");

    this.saveNewMapper();

    return this;
  }

  addSAMLAttributeToRoleMapper(name: string) {
    this.typeName(name);

    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("SAML Attribute to Role")
      .click();

    this.addRoleToMapperForm();

    this.saveNewMapper();

    return this;
  }

  editUsernameTemplateImporterMapper() {
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Legacy");

    cy.findByTestId(this.#template).type("_edited");

    cy.get(this.#target).click().parent().contains("BROKER_USERNAME").click();

    this.saveNewMapper();

    return this;
  }

  editSocialMapper() {
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.findByTestId(this.#socialProfileJSONfieldPath).clear();

    cy.findByTestId(this.#socialProfileJSONfieldPath).type(
      "social profile JSON field path edited",
    );

    cy.findByTestId(this.#userAttributeName).clear();

    cy.findByTestId(this.#userAttributeName).type("user attribute name edited");

    this.saveNewMapper();

    return this;
  }

  editSAMLorOIDCMapper() {
    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#attributesKeyInput).clear();
    cy.get(this.#attributesKeyInput).type("key_edited");

    cy.get(this.#attributesValueInput).clear();
    cy.get(this.#attributesValueInput).type("value_edited");

    this.toggleSwitch(this.#regexAttributeValuesSwitch);

    this.saveNewMapper();

    return this;
  }

  addOIDCAttributeImporterMapper(name: string) {
    this.typeName(name);

    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");
    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect)
      .contains("Attribute Importer")
      .click();

    cy.findByTestId(this.#claimInput).clear();
    cy.findByTestId(this.#claimInput).type("claim");

    cy.findByTestId(this.#userAttributeName).clear();
    cy.findByTestId(this.#userAttributeName).type("user attribute name");

    this.saveNewMapper();

    return this;
  }

  addOIDCClaimToRoleMapper(name: string) {
    this.typeName(name);

    Select.selectItem(cy.get(this.#syncmodeSelectToggle), "Inherit");

    cy.get(this.#idpMapperSelectToggle).click();

    cy.findByTestId(this.#idpMapperSelect).contains("Claim to Role").click();

    cy.findByTestId("claims-add-row").click();
    const keyValue = new LegacyKeyValueInput("config.claims");

    keyValue.fillKeyValue({ key: "key", value: "value" });

    this.toggleSwitch("are.claim.values.regex");

    this.addRoleToMapperForm();
    this.saveNewMapper();

    return this;
  }

  shouldGoToMappersTab() {
    cy.url().should("include", this.#mappersUrl);

    return this;
  }
}
