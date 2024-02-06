import { describe, expect, it, vi } from "vitest";
import {
  arrayToKeyValue,
  keyValueToArray,
  KeyValueType,
} from "./key-value-convert-unique-keys";

vi.mock("react");

describe("Tests the convert functions for attribute input", () => {
  it("converts empty array into form value with unique key", () => {
    const given: KeyValueType[] = [];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({});
  });

  it("converts array into form value with unique key", () => {
    const given = [{ key: "theKey", value: "theValue" }];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({ theKey: "theValue" });
  });

  it("convert only values with unique key", () => {
    const given = [
      { key: "theKey", value: "theValue" },
      { key: "", value: "" },
    ];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({ theKey: "theValue" });
  });

  it("convert empty object to attributes with unique key", () => {
    const given: {
      [key: string]: string;
    } = {};

    //when
    const result = arrayToKeyValue(given);

    //then
    expect(result).toEqual([{ key: "", value: "" }]);
  });

  it("convert object to attributes with unique key", () => {
    const given = { one: "1", two: "2" };

    //when
    const result = arrayToKeyValue(given);

    //then
    expect(result).toEqual([
      { key: "one", value: "1" },
      { key: "two", value: "2" },
      { key: "", value: "" },
    ]);
  });

  it("convert duplicates into array values with unique key", () => {
    const given = [
      { key: "theKey", value: "one" },
      { key: "theKey", value: "two" },
    ];

    //when
    const result = keyValueToArray(given);

    //then
    expect(result).toEqual({ theKey: "two" });
  });
});
