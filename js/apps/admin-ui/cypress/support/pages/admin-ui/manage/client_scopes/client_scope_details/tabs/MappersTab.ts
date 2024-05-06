import CommonPage from "../../../../../CommonPage";

export default class MappersTab extends CommonPage {
  #addMapperBtn = "#mapperAction";
  #fromPredefinedMappersBtn =
    'ul[class="pf-v5-c-menu__list"] > li:nth-child(1) button';
  #byConfigurationBtn =
    'ul[class="pf-v5-c-menu__list"] > li:nth-child(2) button';
  #mapperConfigurationList =
    'ul[aria-label="Add predefined mappers"] > li:not([id=header])';

  #mapperNameInput = "#name";

  addPredefinedMappers(mappersNames: string[]) {
    cy.get(this.#addMapperBtn).click();
    cy.get(this.#fromPredefinedMappersBtn).click();

    this.tableUtils().setTableInModal(true);
    for (const mapperName of mappersNames) {
      this.tableUtils().selectRowItemCheckbox(mapperName);
    }
    this.tableUtils().setTableInModal(false);

    this.modalUtils().confirmModal();
    this.masthead().checkNotificationMessage("Mapping successfully created");
    this.sidebar().waitForPageLoad();
    cy.contains(mappersNames[0]).should("exist");

    for (const mapperName of mappersNames) {
      this.tableUtils().checkRowItemExists(mapperName, true);
    }

    return this;
  }

  addMappersByConfiguration(predefinedMapperName: string, mapperName: string) {
    cy.get(this.#addMapperBtn).click();
    cy.get(this.#byConfigurationBtn).click();

    cy.get(this.#mapperConfigurationList)
      .contains(predefinedMapperName)
      .click();

    cy.get(this.#mapperNameInput).type(mapperName);

    this.formUtils().save();
    this.masthead().checkNotificationMessage("Mapping successfully created");

    return this;
  }

  removeMappers(mappersNames: string[]) {
    for (const mapperName of mappersNames) {
      this.tableUtils().checkRowItemExists(mapperName);
      this.tableUtils().selectRowItemAction(mapperName, "Delete");
      this.sidebar().waitForPageLoad();
      this.masthead().checkNotificationMessage("Mapping successfully deleted");
      this.sidebar().waitForPageLoad();
      this.tableUtils().checkRowItemExists(mapperName, false);
    }

    return this;
  }
}
