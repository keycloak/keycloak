import CommonElements from "../../CommonElements";

export default class TabPage extends CommonElements {
  protected tabItemSelector: string;

  constructor() {
    super(".pf-c-tabs");
    this.tabItemSelector = this.parentSelector + ".pf-c-tabs__item";
  }

  clickTab(tabName: string) {
    cy.get(this.tabItemSelector).contains(tabName).click();
    this.checkIsCurrentTab(tabName);
    return this;
  }

  checkIsCurrentTab(tabName: string) {
    cy.get(this.tabItemSelector)
      .contains(tabName)
      .parent()
      .should("have.class", "pf-m-current");
    return this;
  }

  checkTabExists(tabName: string, exists: boolean) {
    const condition = exists ? "exist" : "not.exist";
    cy.get(this.tabItemSelector).contains(tabName).should(condition);
    return this;
  }

  checkNumberOfTabsIsEqual(number: number) {
    cy.get(this.tabItemSelector).should("have.length", number);
    return this;
  }
}
