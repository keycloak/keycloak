import { mount } from "cypress/react";
import { ConfirmDialogModal } from "../../src/components/confirm-dialog/ConfirmDialog";

describe("ConfirmDialogModal", () => {
  const bodySelector = "#pf-modal-part-2";

  it("should mount", () => {
    const toggle = cy.spy().as("toggleDialogSpy");
    const confirm = cy.spy().as("onConfirmSpy");

    mount(
      <ConfirmDialogModal
        continueButtonLabel="Yes"
        cancelButtonLabel="No"
        titleKey="Hello"
        open
        toggleDialog={toggle}
        onConfirm={confirm}
      >
        Some text
      </ConfirmDialogModal>
    );

    cy.get(bodySelector).should("have.text", "Some text");
    cy.findByTestId("confirm").click();
    cy.get("@onConfirmSpy").should("have.been.called");
    cy.findAllByTestId("cancel").click();
    cy.get("@toggleDialogSpy").should("have.been.called");
  });
});
