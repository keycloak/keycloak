import { expect } from "chai";
import { joinPath } from "../lib/utils/joinPath.js";

describe("joinPath", () => {
  it("returns an empty string when no paths are provided", () => {
    expect(joinPath()).to.equal("");
  });

  it("joins paths", () => {
    expect(joinPath("foo", "bar", "baz")).to.equal("foo/bar/baz");
    expect(joinPath("foo", "/bar", "baz")).to.equal("foo/bar/baz");
    expect(joinPath("foo", "bar/", "baz")).to.equal("foo/bar/baz");
    expect(joinPath("foo", "/bar/", "baz")).to.equal("foo/bar/baz");
  });

  it("joins paths with leading slashes", () => {
    expect(joinPath("/foo", "bar", "baz")).to.equal("/foo/bar/baz");
  });

  it("joins paths with trailing slashes", () => {
    expect(joinPath("foo", "bar", "baz/")).to.equal("foo/bar/baz/");
  });
});
