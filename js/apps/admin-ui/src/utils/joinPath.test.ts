import { describe, expect, it } from "vitest";
import { joinPath } from "./joinPath";

describe("joinPath", () => {
  it("returns an empty string when no paths are provided", () => {
    expect(joinPath()).toBe("");
  });

  it("joins paths", () => {
    expect(joinPath("foo", "bar", "baz")).toBe("foo/bar/baz");
    expect(joinPath("foo", "/bar", "baz")).toBe("foo/bar/baz");
    expect(joinPath("foo", "bar/", "baz")).toBe("foo/bar/baz");
    expect(joinPath("foo", "/bar/", "baz")).toBe("foo/bar/baz");
  });

  it("joins paths with leading slashes", () => {
    expect(joinPath("/foo", "bar", "baz")).toBe("/foo/bar/baz");
  });

  it("joins paths with trailing slashes", () => {
    expect(joinPath("foo", "bar", "baz/")).toBe("foo/bar/baz/");
  });
});
