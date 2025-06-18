import { describe, expect, it } from "vitest";
import { isDefined } from "./isDefined";

describe("isDefined", () => {
  it("detects defined values", () => {
    expect(isDefined(0)).toBe(true);
    expect(isDefined(false)).toBe(true);
    expect(isDefined("")).toBe(true);
  });

  it("detects undefined values", () => {
    expect(isDefined(undefined)).toBe(false);
    expect(isDefined(null)).toBe(false);
  });
});
