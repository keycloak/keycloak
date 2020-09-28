import React from "react";
import { mount } from "enzyme";
import { useConfirmDialog } from "../ConfirmDialog";

describe("Confirmation dialog", () => {
  it("renders simple confirm dialog", () => {
    const onConfirm = jest.fn();
    const Test = () => {
      const [toggle, ConfirmDialog] = useConfirmDialog({
        titleKey: "Delete app02?",
        messageKey:
          "If you delete this client, all associated data will be removed.",
        continueButtonLabel: "Delete",
        onConfirm: onConfirm,
      });
      return (
        <>
          <button id="show" onClick={toggle}>
            Show
          </button>
          <ConfirmDialog />
        </>
      );
    };

    const simple = mount(<Test />);
    simple.find("#show").simulate("click");
    expect(simple).toMatchSnapshot();

    const button = simple.find("#modal-confirm").find("button");
    expect(button).not.toBeNull();

    button!.simulate("click");
    expect(onConfirm).toBeCalled();
  });
});
