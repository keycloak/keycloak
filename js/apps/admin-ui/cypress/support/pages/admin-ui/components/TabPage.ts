import CommonElements from "../../CommonElements";

export default class TabPage extends CommonElements {
  protected tabItemSelector: string;
  protected tabsList: string;

  constructor() {
    super(".pf-v5-c-tabs");
    this.tabItemSelector = ".pf-v5-c-tabs__item";
    this.tabsList = '[role="tablist"]';
  }

  #getTab(tabName: string, index: number | undefined = 0) {
    return cy
      .get(this.parentSelector)
      .eq(index)
      .find(this.tabItemSelector)
      .contains(tabName);
  }

  clickTab(tabName: string, index: number | undefined = 0) {
    this.#getTab(tabName, index).click();
    this.checkIsCurrentTab(tabName, index);
    return this;
  }

  checkIsCurrentTab(tabName: string, index: number | undefined = 0) {
    this.#getTab(tabName, index).parent().should("have.class", "pf-m-current");
    return this;
  }

  checkTabExists(
    tabName: string,
    exists: boolean,
    index: number | undefined = 0,
  ) {
    const condition = exists ? "exist" : "not.exist";
    this.#getTab(tabName, index).should(condition);
    return this;
  }

  checkNumberOfTabsIsEqual(number: number) {
    cy.get(this.tabsList).find("li").should("have.length", number);
    return this;
  }
}
