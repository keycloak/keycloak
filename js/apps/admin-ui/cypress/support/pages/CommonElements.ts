import { trim } from "lodash-es";

export default class CommonElements {
  protected parentSelector;
  protected primaryBtn;
  protected secondaryBtn;
  protected secondaryBtnLink;
  protected dropdownMenuItem;
  protected selectMenuItem;
  protected dropdownToggleBtn;
  protected dropdownSelectToggleBtn;
  protected dropdownSelectToggleItem;
  protected tableKebabBtn;

  constructor(parentSelector = "") {
    this.parentSelector = trim(parentSelector) + " ";
    this.primaryBtn = this.parentSelector + ".pf-v5-c-button.pf-m-primary";
    this.secondaryBtn = this.parentSelector + ".pf-v5-c-button.pf-m-secondary";
    this.secondaryBtnLink = this.parentSelector + ".pf-v5-c-button.pf-m-link";
    this.dropdownMenuItem =
      this.parentSelector + ".pf-v5-c-menu__list .pf-v5-c-menu__item";
    this.selectMenuItem =
      this.parentSelector + ".pf-v5-c-menu__list .pf-v5-c-menu__list-item";
    this.dropdownToggleBtn = this.parentSelector + ".pf-v5-c-menu-toggle";
    this.tableKebabBtn = this.parentSelector + ".pf-v5-c-menu-toggle";
    this.dropdownSelectToggleBtn = this.parentSelector + ".pf-v5-c-menu-toggle";
    this.dropdownSelectToggleItem =
      this.parentSelector + ".pf-v5-c-menu__list > li";
  }

  clickPrimaryBtn() {
    cy.get(this.primaryBtn).click();
    return this;
  }

  clickSecondaryBtn(buttonName: string, force = false) {
    cy.get(this.secondaryBtn).contains(buttonName).click({ force: force });
    return this;
  }

  checkIfExists(exist: boolean) {
    cy.get(this.parentSelector).should((!exist ? "not." : "") + "exist");
    return this;
  }

  checkElementIsDisabled(
    element: Cypress.Chainable<JQuery<HTMLElement>>,
    disabled: boolean,
  ) {
    element.then(($btn) => {
      if ($btn.hasClass("pf-m-disabled")) {
        element.should(
          (!disabled ? "not." : "") + "have.class",
          "pf-m-disabled",
        );
      } else {
        element.should((!disabled ? "not." : "") + "have.attr", "disabled");
      }
    });
  }
}
