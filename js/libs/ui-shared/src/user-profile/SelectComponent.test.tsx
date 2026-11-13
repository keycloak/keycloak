import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { TFunction } from "i18next";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { afterEach, describe, expect, it } from "vitest";
import { SelectComponent } from "./SelectComponent";
import { UserFormFields } from "./utils";

const t = ((key: string) => key) as TFunction;

const Value = () => (
  <output data-testid="value">
    {JSON.stringify(useWatch({ name: "attributes.l2" }))}
  </output>
);

const Harness = ({ value = ["ops"] }: { value?: string[] }) => {
  const form = useForm<UserFormFields>({
    defaultValues: { attributes: { l2: value } },
  });

  return (
    <FormProvider {...form}>
      <SelectComponent
        t={t}
        form={form}
        inputType="multiselect"
        attribute={{
          name: "l2",
          displayName: "L2",
          required: false,
          readOnly: false,
          validators: {
            options: { options: ["ops", "foo"] },
          },
          annotations: {
            inputType: "multiselect",
            inputOptionLabels: {
              ops: "OPS56",
              foo: "FOO",
            },
          },
        }}
      />
      <Value />
    </FormProvider>
  );
};

const formValue = () => screen.getByTestId("value").textContent;
const chipGroup = () => document.querySelector(".pf-v5-c-chip-group")!;
const removeChip = () =>
  fireEvent.click(
    within(chipGroup() as HTMLElement).getByRole("button", {
      name: /close/i,
    }),
  );

describe("SelectComponent multiselect chips", () => {
  afterEach(cleanup);

  it("shows the option label on the chip", () => {
    render(<Harness />);

    expect(within(chipGroup() as HTMLElement).getByText("OPS56")).toBeTruthy();
    expect(formValue()).toBe('["ops"]');
  });

  it("removes the selected option key when the chip is cleared", () => {
    render(<Harness />);

    removeChip();

    expect(chipGroup()).toBeNull();
    expect(formValue()).toBe("[]");
  });

  it("does not add the label string when clearing a labeled chip", () => {
    render(<Harness />);

    removeChip();

    expect(formValue()).not.toContain("OPS56");
    expect(formValue()).toBe("[]");
  });
});
