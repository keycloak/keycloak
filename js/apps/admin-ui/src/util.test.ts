import { describe, expect, it, vi } from "vitest";
import {
  convertAttributeNameToForm,
  convertFormValuesToObject,
  convertToFormValues,
} from "./util";

vi.mock("react");

const TOKEN = "🍺";

describe("Tests the form convert util functions", () => {
  it("convert to form values", () => {
    const given = {
      name: "client",
      other: { one: "1", two: "2" },
      attributes: { one: ["1"] },
    };
    const values: { [index: string]: any } = {};
    const spy = (name: string, value: any) => (values[name] = value);

    //when
    convertToFormValues(given, spy);

    //then
    expect(values).toEqual({
      name: "client",
      other: { one: "1", two: "2" },
      attributes: [{ key: "one", value: "1" }],
    });
  });

  it("convert save values", () => {
    const given = {
      name: "client",
      attributes: [{ key: "one", value: "1" }],
      config: { [`one${TOKEN}two`]: "3" },
    };

    //when
    const values = convertFormValuesToObject(given);

    //then
    expect(values).toEqual({
      name: "client",
      attributes: { one: ["1"] },
      config: { "one.two": "3" },
    });
  });

  it("convert attributes flatten", () => {
    const given = {
      name: "test",
      description: "",
      type: "default",
      attributes: {
        [`display${TOKEN}on${TOKEN}consent${TOKEN}screen`]: "true",
        [`include${TOKEN}in${TOKEN}token${TOKEN}scope`]: "true",
        [`gui${TOKEN}order`]: "1",
        [`consent${TOKEN}screen${TOKEN}text`]: "",
      },
    };

    //when
    const values = convertFormValuesToObject(given);

    //then
    expect(values).toEqual({
      name: "test",
      description: "",
      type: "default",
      attributes: {
        "display.on.consent.screen": "true",
        "include.in.token.scope": "true",
        "gui.order": "1",
        "consent.screen.text": "",
      },
    });
  });

  it("convert flatten attributes to object", () => {
    const given = {
      attributes: {
        "display.on.consent.screen": "true",
        "include.in.token.scope": "true",
        "gui.order": "1",
        "consent.screen.text": "",
      },
    };
    const values: { [index: string]: any } = {};
    const spy = (name: string, value: any) => (values[name] = value);

    //when
    convertToFormValues(given, spy);

    //then
    expect(values).toEqual({
      [`attributes.display${TOKEN}on${TOKEN}consent${TOKEN}screen`]: "true",
      [`attributes.include${TOKEN}in${TOKEN}token${TOKEN}scope`]: "true",
      [`attributes.gui${TOKEN}order`]: "1",
      [`attributes.consent${TOKEN}screen${TOKEN}text`]: "",
    });
  });

  it("convert empty to empty object", () => {
    const given = { attributes: [{ key: "", value: "" }] };

    //when
    const values = convertFormValuesToObject(given);

    //then
    expect(values).toEqual({
      attributes: {},
    });
  });

  it("convert single element arrays to string", () => {
    const given = {
      config: {
        group: ["one"],
        "another.nested": ["value"],
      },
    };
    const values: { [index: string]: any } = {};
    const spy = (name: string, value: any) => (values[name] = value);

    //when
    convertToFormValues(given, spy);

    //then
    expect(values).toEqual({
      "config.group": "one",
      [`config.another${TOKEN}nested`]: "value",
    });
  });

  it("should convert attribute name to form", () => {
    const given = "attributes.some.strange.attribute";

    //when
    const form = convertAttributeNameToForm(given);

    //then
    expect(form).toEqual(`attributes.some${TOKEN}strange${TOKEN}attribute`);
  });
});
