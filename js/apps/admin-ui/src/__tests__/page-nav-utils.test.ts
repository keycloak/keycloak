import { describe, expect, it } from "vitest";
import { normalizeNavRoutePath } from "../page-nav-utils";

describe("normalizeNavRoutePath", () => {
  it("matches splat routes to their base nav path", () => {
    expect(normalizeNavRoutePath("/:realm/groups/*")).toBe("/groups");
  });

  it("matches parameterized routes without splats", () => {
    expect(normalizeNavRoutePath("/:realm/clients")).toBe("/clients");
    expect(normalizeNavRoutePath("/:realm/groups/:id")).toBe("/groups");
  });
});
