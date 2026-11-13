import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { FormProvider, useForm } from "react-hook-form";
import { afterEach, describe, expect, it } from "vitest";
import { TextControl } from "./TextControl";

const Form = () => {
  const form = useForm<{ name: string }>({ defaultValues: { name: "" } });

  return (
    <FormProvider {...form}>
      <TextControl name="name" label="Name" aria-describedby="name-hint" />
      <button
        type="button"
        onClick={() => form.setError("name", { message: "Name is required" })}
      >
        Show error
      </button>
      <button type="button" onClick={() => form.clearErrors("name")}>
        Clear error
      </button>
    </FormProvider>
  );
};

describe("TextControl error accessibility", () => {
  afterEach(cleanup);

  it("links the invalid field to its error and removes the link when cleared", async () => {
    render(<Form />);
    const input = screen.getByRole("textbox", { name: "Name" });

    expect(input.getAttribute("aria-describedby")).toBe("name-hint");
    expect(input.getAttribute("aria-errormessage")).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Show error" }));

    const error = await screen.findByText("Name is required");
    expect(input.getAttribute("aria-invalid")).toBe("true");
    expect(input.getAttribute("aria-errormessage")).toBe("name-error");
    expect(document.getElementById("name-error")?.contains(error)).toBe(true);
    expect(input.getAttribute("aria-describedby")).toBe("name-hint");

    fireEvent.click(screen.getByRole("button", { name: "Clear error" }));

    expect(input.getAttribute("aria-invalid")).toBe("false");
    expect(input.getAttribute("aria-errormessage")).toBeNull();
    expect(document.getElementById("name-error")).toBeNull();
  });
});
