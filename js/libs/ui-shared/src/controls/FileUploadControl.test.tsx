import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { FormProvider, useForm } from "react-hook-form";
import { StrictMode } from "react";
import { afterEach, describe, expect, it } from "vitest";
import { FileUploadControl } from "./FileUploadControl";

const Harness = () => {
  const form = useForm({ defaultValues: { file: "" } });
  return (
    <FormProvider {...form}>
      <FileUploadControl name="file" id="importFile" label="Import file" />
      <button
        type="button"
        onClick={() => form.setError("file", { message: "Choose a file" })}
      >
        Show error
      </button>
      <button type="button" onClick={() => form.clearErrors("file")}>
        Clear error
      </button>
    </FormProvider>
  );
};

describe("FileUploadControl", () => {
  afterEach(cleanup);

  it("links the visible filename input to its error", async () => {
    render(
      <StrictMode>
        <Harness />
      </StrictMode>,
    );
    const input = screen.getByRole<HTMLInputElement>("textbox", {
      name: "Import file",
    });

    expect(input.getAttribute("aria-invalid")).toBe("false");
    expect(input.hasAttribute("aria-errormessage")).toBe(false);

    fireEvent.click(screen.getByRole("button", { name: "Show error" }));

    const error = await screen.findByText("Choose a file");
    expect(input.getAttribute("aria-invalid")).toBe("true");
    expect(input.getAttribute("aria-errormessage")).toBe(
      "importFile-filename-error",
    );
    expect(
      document.getElementById("importFile-filename-error")?.contains(error),
    ).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "Clear error" }));
    expect(input.getAttribute("aria-invalid")).toBe("false");
    expect(input.hasAttribute("aria-errormessage")).toBe(false);
  });

  it("updates only the file field with an error", () => {
    const TwoFiles = () => {
      const form = useForm({ defaultValues: { first: "", second: "" } });
      return (
        <FormProvider {...form}>
          <FileUploadControl name="first" id="firstFile" label="First file" />
          <FileUploadControl
            name="second"
            id="secondFile"
            label="Second file"
          />
          <button
            type="button"
            onClick={() =>
              form.setError("second", { message: "Choose the second file" })
            }
          >
            Show second error
          </button>
        </FormProvider>
      );
    };

    render(<TwoFiles />);
    fireEvent.click(screen.getByRole("button", { name: "Show second error" }));

    const first = screen.getByRole("textbox", { name: "First file" });
    const second = screen.getByRole("textbox", { name: "Second file" });
    expect(first.getAttribute("aria-invalid")).toBe("false");
    expect(first.hasAttribute("aria-errormessage")).toBe(false);
    expect(second.getAttribute("aria-invalid")).toBe("true");
    expect(second.getAttribute("aria-errormessage")).toBe(
      "secondFile-filename-error",
    );
  });

  it("still selects and clears a file", async () => {
    render(<Harness />);
    const fileInput = screen
      .getByTestId("file")
      .querySelector<HTMLInputElement>('input[type="file"]');
    expect(fileInput).not.toBeNull();

    fireEvent.change(fileInput!, {
      target: { files: [new File(["data"], "sample.pem")] },
    });
    await waitFor(() =>
      expect(screen.getByRole<HTMLInputElement>("textbox").value).toBe(
        "sample.pem",
      ),
    );

    fireEvent.click(screen.getByRole("button", { name: "Clear" }));
    expect(screen.getByRole<HTMLInputElement>("textbox").value).toBe("");
  });
});
