import ModalUtils from "../../../../util/ModalUtils";

export default class BindFlowModal extends ModalUtils {
  private bindingType = "#chooseBindingType";

  fill(bindingType: string) {
    cy.get(this.bindingType).click();
    cy.get(this.dropdownSelectToggleItem).contains(bindingType).click();
    return this;
  }

  save() {
    cy.findAllByTestId("save").click();

    return this;
  }
}
