import { describe, expect, it } from "vitest";
import { toUserFormFields, toUserRepresentation } from "./form-state";

const metadata = {
  attributes: [
    { name: "username" },
    { name: "email" },
    {
      name: "l2",
      multivalued: false,
      annotations: {
        inputType: "multiselect",
        inputOptionLabels: { foo: "FOO", ops: "OPS56" },
      },
    },
    {
      name: "tags",
      multivalued: true,
    },
  ],
};

describe("toUserFormFields", () => {
  it("seeds cleared multiselect attributes as empty arrays so reset drops chips", () => {
    const fields = toUserFormFields(
      {
        username: "qrqwe",
        // Server omits cleared attributes from the representation.
        attributes: undefined,
      },
      metadata,
    );

    expect(fields.attributes).toEqual({
      l2: [],
      tags: [],
    });
  });

  it("keeps server values over empty seeds", () => {
    const fields = toUserFormFields(
      {
        username: "qrqwe",
        attributes: { l2: ["foo"] },
      },
      metadata,
    );

    expect(fields.attributes).toEqual({
      l2: ["foo"],
      tags: [],
    });
  });

  it("does not put root attributes into the attributes map", () => {
    const fields = toUserFormFields(
      { username: "qrqwe", email: "a@b.c", attributes: {} },
      metadata,
    );

    expect(fields.attributes).toEqual({
      l2: [],
      tags: [],
    });
    expect(fields.username).toBe("qrqwe");
    expect(fields.email).toBe("a@b.c");
  });
});

describe("toUserRepresentation", () => {
  it("preserves empty arrays so the server clears the attribute", () => {
    const user = toUserRepresentation({
      username: "qrqwe",
      attributes: { l2: [] },
    });

    expect(user.attributes).toEqual({ l2: [] });
  });
});
