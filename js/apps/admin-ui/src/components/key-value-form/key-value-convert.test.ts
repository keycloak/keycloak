import { describe, expect, it, vi } from "vitest";
import {
  arrayToKeyValue,
  keyValueToArray,
  KeyValueType,
} from "./key-value-convert";

vi.mock("react");

describe("Tests the convert functions for attribute input", () => {
  it("converts empty array into form value", () => {
    const given: KeyValueType[] = [];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({});
  });

  it("converts array into form value", () => {
    const given = [{ key: "theKey", value: "theValue" }];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({ theKey: ["theValue"] });
  });

  it("convert only values", () => {
    const given = [
      { key: "theKey", value: "theValue" },
      { key: "", value: "" },
    ];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({ theKey: ["theValue"] });
  });

  it("convert object to attributes", () => {
    const given = { one: ["1"], two: ["2"] };

    //when
    const result = arrayToKeyValue(given);

    //then
    expect(result).toEqual([
      { key: "one", value: "1" },
      { key: "two", value: "2" },
    ]);
  });

  it("convert duplicates into array values", () => {
    const given = [
      { key: "theKey", value: "one" },
      { key: "theKey", value: "two" },
    ];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({ theKey: ["one", "two"] });
  });

  it("converts prototype-name key safely", () => {
    const result = keyValueToArray([{ key: "toString", value: "x" }]);

    expect(Object.hasOwn(result, "toString")).toBe(true);
    expect(result["toString"]).toEqual(["x"]);
    expect(Object.getPrototypeOf(result)).toBeNull();
  });

  it("converts duplicate prototype-name keys safely", () => {
    const result = keyValueToArray([
      { key: "constructor", value: "a" },
      { key: "constructor", value: "b" },
    ]);

    expect(Object.hasOwn(result, "constructor")).toBe(true);
    expect(result["constructor"]).toEqual(["a", "b"]);
    expect(Object.getPrototypeOf(result)).toBeNull();
  });

  it("converts __proto__ key without mutating prototype", () => {
    const result = keyValueToArray([{ key: "__proto__", value: "x" }]);

    expect(Object.hasOwn(result, "__proto__")).toBe(true);
    expect(result["__proto__"]).toEqual(["x"]);
    expect(Object.getPrototypeOf(result)).toBeNull();
  });
});
