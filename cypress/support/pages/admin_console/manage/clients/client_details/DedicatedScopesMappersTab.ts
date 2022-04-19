import CommonPage from "../../../../CommonPage";

enum mapperType {
  FromPredefinedMappers = "From predefined mappers",
  ByConfiguration = "By configuration",
}

enum mapperTypeEmptyState {
  AddPredefinedMapper = "Add predefined mapper",
  ConfigureaNewMapper = "Configure a new mapper",
}

export default class DedicatedScopesMappersTab extends CommonPage {
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
    this.emptyState()
      .checkIfExists(true)
      .clickSecondaryBtn(mapperTypeEmptyState.AddPredefinedMapper);
    return this;
  }

  configureNewMapper() {
    this.emptyState()
      .checkIfExists(true)
      .clickSecondaryBtn(mapperTypeEmptyState.ConfigureaNewMapper);
    return this;
  }
}
