import type { Context } from "react";
import { useContext } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useRequiredContext } from "./useRequiredContext";

vi.mock("react");

const useContextMock = vi.mocked(useContext);

describe("useRequiredContext", () => {
  beforeEach(() => {
    useContextMock.mockReset();
  });

  it("resolves the context", () => {
    const context = {} as Context<unknown>;
    const resolved = "FakeValue";

    useContextMock.mockReturnValue(resolved);

    expect(useRequiredContext(context)).toEqual(resolved);
  });

  it("throws if a named context cannot be resolved", () => {
    const displayName = "FakeDisplayName";
    const context = { displayName } as Context<unknown>;
    const expected = `No provider found for the '${displayName}' context, make sure it is included in your component hierarchy.`;

    useContextMock.mockReturnValue(undefined);
    expect(() => useRequiredContext(context)).toThrow(expected);

    useContextMock.mockReturnValue(null);
    expect(() => useRequiredContext(context)).toThrow(expected);
  });

  it("throws if an unnamed context cannot be resolved", () => {
    const context = {} as Context<unknown>;
    const expected =
      "No provider found for an unknown context, make sure it is included in your component hierarchy.";

    useContextMock.mockReturnValue(undefined);
    expect(() => useRequiredContext(context)).toThrow(expected);

    useContextMock.mockReturnValue(null);
    expect(() => useRequiredContext(context)).toThrow(expected);
  });
});
