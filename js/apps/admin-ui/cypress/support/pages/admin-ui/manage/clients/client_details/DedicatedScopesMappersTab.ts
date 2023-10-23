import CommonPage from "../../../../CommonPage";

enum mapperType {
  FromPredefinedMappers = "From predefined mappers",
  ByConfiguration = "By configuration",
}

export default class DedicatedScopesMappersTab extends CommonPage {
  #addPredefinedMapperEmptyStateBtn = "add-predefined-mapper-empty-action";
  #configureNewMapperEmptyStateBtn = "configure-a-new-mapper-empty-action";

  addMapperFromPredefinedMappers() {
    this.emptyState().checkIfExists(false);
    this.tableToolbarUtils()
      .addMapper()
      .clickDropdownMenuItem(mapperType.ByConfiguration);
    return this;
  }

  addMapperByConfiguration() {
    this.emptyState().checkIfExists(false);
    this.tableToolbarUtils()
      .addMapper()
      .clickDropdownMenuItem(mapperType.FromPredefinedMappers);
    return this;
  }

  addPredefinedMapper() {
    this.emptyState().checkIfExists(true);
    cy.findByTestId(this.#addPredefinedMapperEmptyStateBtn).click();
    return this;
  }

  configureNewMapper() {
    this.emptyState().checkIfExists(true);
    cy.findByTestId(this.#configureNewMapperEmptyStateBtn).click({
      force: true,
    });
    return this;
  }
}
