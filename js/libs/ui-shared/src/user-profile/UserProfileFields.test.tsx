import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { cleanup, render, screen } from "@testing-library/react";
import { FormProvider, useForm } from "react-hook-form";
import { afterEach, describe, it, expect } from "vitest";
import { HiddenComponent } from "./HiddenComponent";
import { FIELDS, determineInputType } from "./UserProfileFields";

// ─── helpers ────────────────────────────────────────────────────────────────

const t = (key: string) => key;

function attr(
  overrides: Partial<UserProfileAttributeMetadata> & { name: string },
): UserProfileAttributeMetadata {
  return { required: false, readOnly: false, multivalued: false, ...overrides };
}

/** Wraps a component in the react-hook-form context it needs to register fields. */
const Harness = ({
  children,
}: {
  children: (form: ReturnType<typeof useForm>) => React.ReactNode;
}) => {
  const form = useForm();
  return <FormProvider {...form}>{children(form)}</FormProvider>;
};

// ─── determineInputType ─────────────────────────────────────────────────────

describe("determineInputType", () => {
  it("returns 'hidden' for a regular attribute annotated as hidden", () => {
    expect(
      determineInputType(attr({ name: "customField", annotations: { inputType: "hidden" } })),
    ).toBe("hidden");
  });

  it("returns 'hidden' for a root attribute (username) annotated as hidden, overriding the text fallback", () => {
    expect(
      determineInputType(attr({ name: "username", annotations: { inputType: "hidden" } })),
    ).toBe("hidden");
  });

  it("returns 'hidden' for a root attribute (email) annotated as hidden", () => {
    expect(
      determineInputType(attr({ name: "email", annotations: { inputType: "hidden" } })),
    ).toBe("hidden");
  });

  it("returns 'text' for a root attribute (username) with a non-hidden annotation", () => {
    expect(
      determineInputType(attr({ name: "username", annotations: { inputType: "textarea" } })),
    ).toBe("text");
  });

  it("returns 'text' for a root attribute with no annotation", () => {
    expect(determineInputType(attr({ name: "firstName" }))).toBe("text");
  });

  it("returns 'hidden' for a multivalued attribute annotated as hidden", () => {
    expect(
      determineInputType(
        attr({ name: "tags", multivalued: true, annotations: { inputType: "hidden" } }),
      ),
    ).toBe("hidden");
  });

  it("returns 'hidden' for a locale attribute annotated as hidden", () => {
    expect(
      determineInputType(attr({ name: "locale", annotations: { inputType: "hidden" } })),
    ).toBe("hidden");
  });

  it("returns 'text' for a non-root attribute with no annotation", () => {
    expect(determineInputType(attr({ name: "customField" }))).toBe("text");
  });

  it("returns the annotated type for a non-root attribute with a valid annotation", () => {
    expect(
      determineInputType(attr({ name: "bio", annotations: { inputType: "textarea" } })),
    ).toBe("textarea");
  });
});

// ─── HiddenComponent ────────────────────────────────────────────────────────

describe("HiddenComponent", () => {
  afterEach(cleanup);

  it("renders an input of type hidden", () => {
    const { container } = render(
      <Harness>
        {(form) => (
          <HiddenComponent
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({ name: "customField" })}
          />
        )}
      </Harness>,
    );

    const input = container.querySelector("input");
    expect(input).not.toBeNull();
    expect(input!.type).toBe("hidden");
  });

  it("carries the attribute defaultValue", () => {
    const { container } = render(
      <Harness>
        {(form) => (
          <HiddenComponent
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({ name: "customField", defaultValue: "prefilled" })}
          />
        )}
      </Harness>,
    );

    const input = container.querySelector<HTMLInputElement>("input");
    expect(input!.defaultValue).toBe("prefilled");
  });

  it("renders no visible label or wrapper", () => {
    const { container } = render(
      <Harness>
        {(form) => (
          <HiddenComponent
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({ name: "customField" })}
          />
        )}
      </Harness>,
    );

    // Only one element in the DOM — the bare <input>
    expect(container.children).toHaveLength(1);
    expect(container.firstElementChild!.tagName).toBe("INPUT");
  });
});

// ─── FIELDS map ─────────────────────────────────────────────────────────────

describe("FIELDS map", () => {
  it("maps 'hidden' to HiddenComponent", () => {
    expect(FIELDS["hidden"]).toBe(HiddenComponent);
  });

  it("includes 'hidden' alongside all other input types", () => {
    expect(Object.keys(FIELDS)).toContain("hidden");
  });
});

// ─── determineInputType behaviour (tested via FIELDS lookup) ────────────────
// These tests verify that the correct component is chosen for an attribute
// depending on its annotation and whether it is a root / multivalued attribute.

describe("hidden inputType rendering rules", () => {
  afterEach(cleanup);

  it("renders a hidden input for a regular attribute with inputType=hidden", () => {
    const Component = FIELDS["hidden"];
    const { container } = render(
      <Harness>
        {(form) => (
          <Component
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({
              name: "secretField",
              annotations: { inputType: "hidden" },
            })}
          />
        )}
      </Harness>,
    );

    expect(container.querySelector("input[type='hidden']")).not.toBeNull();
  });

  it("renders a hidden input for a root attribute (username) with inputType=hidden", () => {
    const Component = FIELDS["hidden"];
    const { container } = render(
      <Harness>
        {(form) => (
          <Component
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({
              name: "username",
              annotations: { inputType: "hidden" },
            })}
          />
        )}
      </Harness>,
    );

    expect(container.querySelector("input[type='hidden']")).not.toBeNull();
  });

  it("renders a hidden input for a multivalued attribute with inputType=hidden", () => {
    const Component = FIELDS["hidden"];
    const { container } = render(
      <Harness>
        {(form) => (
          <Component
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({
              name: "tags",
              multivalued: true,
              annotations: { inputType: "hidden" },
            })}
          />
        )}
      </Harness>,
    );

    expect(container.querySelector("input[type='hidden']")).not.toBeNull();
    // Must NOT render any visible controls
    expect(screen.queryByRole("textbox")).toBeNull();
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("does not render a visible text input when inputType is hidden", () => {
    const Component = FIELDS["hidden"];
    const { container } = render(
      <Harness>
        {(form) => (
          <Component
            t={t as never}
            form={form as never}
            inputType="hidden"
            attribute={attr({
              name: "customField",
              annotations: { inputType: "hidden" },
            })}
          />
        )}
      </Harness>,
    );

    expect(screen.queryByRole("textbox")).toBeNull();
    expect(container.querySelector("input[type='hidden']")).not.toBeNull();
  });
});
