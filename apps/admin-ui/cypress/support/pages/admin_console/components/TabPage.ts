import CommonElements from "../../CommonElements";

export default class TabPage extends CommonElements {
  protected tabItemSelector: string;

  constructor() {
    super(".pf-c-tabs");
    this.tabItemSelector = ".pf-c-tabs__item";
  }

  private getTab(tabName: string, index: number | undefined = 0) {
    return cy
      .get(this.parentSelector)
      .eq(index)
      .find(this.tabItemSelector)
      .contains(tabName);
  }

  clickTab(tabName: string, index: number | undefined = 0) {
    this.getTab(tabName, index).click();
    this.checkIsCurrentTab(tabName, index);
    return this;
  }

  checkIsCurrentTab(tabName: string, index: number | undefined = 0) {
    this.getTab(tabName, index).parent().should("have.class", "pf-m-current");
    return this;
  }

  checkTabExists(
    tabName: string,
    exists: boolean,
    index: number | undefined = 0
  ) {
    const condition = exists ? "exist" : "not.exist";
    this.getTab(tabName, index).should(condition);
    return this;
  }

  checkNumberOfTabsIsEqual(number: number, index: number | undefined = 0) {
    cy.get(this.parentSelector).eq(index).should("have.length", number);
    return this;
  }
}
