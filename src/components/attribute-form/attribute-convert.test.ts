import {
  arrayToAttributes,
  attributesToArray,
  KeyValueType,
} from "./attribute-convert";

jest.mock("react");

describe("Tests the convert functions for attribute input", () => {
  it("converts empty array into form value", () => {
    const given: KeyValueType[] = [];

    //when
    const result = arrayToAttributes(given);

    //then
    expect(result).toEqual({});
  });

  it("converts array into form value", () => {
    const given = [{ key: "theKey", value: "theValue" }];

    //when
    const result = arrayToAttributes(given);

    //then
    expect(result).toEqual({ theKey: ["theValue"] });
  });

  it("convert only values", () => {
    const given = [
      { key: "theKey", value: "theValue" },
      { key: "", value: "" },
    ];

    //when
    const result = arrayToAttributes(given);

    //then
    expect(result).toEqual({ theKey: ["theValue"] });
  });

  it("convert empty object to attributes", () => {
    const given: {
      [key: string]: string[];
    } = {};

    //when
    const result = attributesToArray(given);

    //then
    expect(result).toEqual([{ key: "", value: "" }]);
  });

  it("convert object to attributes", () => {
    const given = { one: ["1"], two: ["2"] };

    //when
    const result = attributesToArray(given);

    //then
    expect(result).toEqual([
      { key: "one", value: "1" },
      { key: "two", value: "2" },
      { key: "", value: "" },
    ]);
  });
});
