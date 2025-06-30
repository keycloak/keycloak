import UserProfile from "./UserProfile";
import Select from "../../../../forms/Select";

export default class ValidatorConfigDialogue {
  readonly #validatorSelector = "#validator";
  readonly #saveValidatorButton = "save-validator-role-button";
  readonly #cancelValidatorButton = "cancel-validator-role-button";
  readonly #addValue = "addValue";

  readonly userProfile: UserProfile;

  constructor(userProfile: UserProfile) {
    this.userProfile = userProfile;
  }

  clickSave() {
    cy.findByTestId(this.#saveValidatorButton).click();

    return this.userProfile;
  }

  selectValidatorType(type: string) {
    Select.selectItem(cy.get(this.#validatorSelector), type);

    return this;
  }

  setListFieldValues(fieldName: string, values: string[]) {
    for (let i = 0; i < values.length; i++) {
      if (i != 0) {
        cy.findByTestId(this.#addValue).click();
      }

      const testId = `config.options${i}`;
      cy.findByTestId(testId).clear().type(values[i]);
    }

    return this;
  }

  clickCancel() {
    cy.findByTestId(this.#cancelValidatorButton).click();

    return this.userProfile;
  }
}
