import { expect } from "chai";
import { stringifyQueryParams } from "../src/utils/stringifyQueryParams.js";

describe("stringifyQueryParams", () => {
  it("ignores undefined and null", () => {
    expect(stringifyQueryParams({ foo: undefined, bar: null })).to.equal("");
  });

  it("ignores empty strings", () => {
    expect(stringifyQueryParams({ foo: "" })).to.equal("");
  });

  it("ignores empty arrays", () => {
    expect(stringifyQueryParams({ foo: [] })).to.equal("");
  });

  it("accepts all other values", () => {
    expect(
      stringifyQueryParams({
        boolTrue: true,
        boolFalse: false,
        numPositive: 1,
        numZero: 0,
        numNegative: -1,
        str: "Hello World!",
        arr: ["foo", "bar"],
      }),
    ).to.equal(
      "boolTrue=true&boolFalse=false&numPositive=1&numZero=0&numNegative=-1&str=Hello+World%21&arr=foo&arr=bar",
    );
  });
});
