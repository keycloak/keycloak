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
});
