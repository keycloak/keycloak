import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { afterEach, describe, expect, it } from "vitest";
import { NumberControl } from "./NumberControl";

/**
 * A value as it comes out of a Keycloak representation. `ComponentRepresentation`,
 * `IdentityProviderRepresentation.config` and the realm attribute map all carry
 * their numbers as strings, so this is what the control is handed in practice.
 */
const STORED = "50";

const Value = () => (
  <output data-testid="value">
    {JSON.stringify(useWatch({ name: "amount" }))}
  </output>
);

const Harness = ({ value, min }: { value?: unknown; min?: number }) => {
  const form = useForm({ defaultValues: { amount: value } });
  return (
    <FormProvider {...form}>
      <NumberControl
        name="amount"
        controller={{
          defaultValue: 0,
          ...(min === undefined ? {} : { rules: { min } }),
        }}
      />
      <Value />
    </FormProvider>
  );
};

const input = () => screen.getByRole<HTMLInputElement>("spinbutton");
const button = (name: RegExp) =>
  screen.getByRole<HTMLButtonElement>("button", { name });
const press = (name: RegExp) => fireEvent.click(button(name));
const formValue = () => screen.getByTestId("value").textContent;

describe("NumberControl", () => {
  afterEach(cleanup);

  describe.each([
    ["a string value", STORED],
    ["a number value", Number(STORED)],
  ])("with %s", (_label, value) => {
    it("shows it", () => {
      render(<Harness value={value} min={0} />);

      expect(input().value).toBe("50");
    });

    it("offers both steppers", () => {
      render(<Harness value={value} min={0} />);

      expect(button(/plus/i).disabled).toBe(false);
      expect(button(/minus/i).disabled).toBe(false);
    });

    it("increments by one", () => {
      render(<Harness value={value} min={0} />);

      press(/plus/i);

      expect(input().value).toBe("51");
      expect(formValue()).toBe("51");
    });

    it("decrements by one", () => {
      render(<Harness value={value} min={0} />);

      press(/minus/i);

      expect(input().value).toBe("49");
      expect(formValue()).toBe("49");
    });
  });

  it("keeps incrementing by one", () => {
    render(<Harness value={STORED} min={0} />);

    press(/plus/i);
    press(/plus/i);
    press(/plus/i);

    expect(input().value).toBe("53");
  });

  it("increments a string value without a min rule", () => {
    render(<Harness value={STORED} />);

    press(/plus/i);

    expect(input().value).toBe("51");
  });

  it("does not step below the min rule", () => {
    render(<Harness value={"1"} min={1} />);

    expect(button(/minus/i).disabled).toBe(true);
    expect(input().value).toBe("1");
  });

  it("clamps a decrement to the min rule", () => {
    render(<Harness value={"2"} min={1} />);

    press(/minus/i);
    press(/minus/i);

    expect(input().value).toBe("1");
  });

  it("takes a typed value over the stored one", () => {
    render(<Harness value={STORED} min={0} />);

    fireEvent.change(input(), { target: { value: "7" } });
    press(/plus/i);

    expect(input().value).toBe("8");
  });

  it("falls back to the default value when the form holds nothing", () => {
    render(<Harness min={0} />);

    expect(input().value).toBe("0");

    press(/plus/i);

    expect(input().value).toBe("1");
  });

  it("leaves an empty value empty", () => {
    render(<Harness value={""} min={0} />);

    expect(input().value).toBe("");
  });

  it("leaves an unparseable value alone", () => {
    render(<Harness value={"no number"} min={0} />);

    // A `number` input cannot render it, but it is passed through rather than
    // converted, so nothing is written back over what is stored.
    expect(input().value).toBe("");
    expect(formValue()).toBe('"no number"');
  });
});
