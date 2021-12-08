import { convertFormValuesToObject, convertToFormValues } from "./util";

jest.mock("react");

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
      attributes: [
        { key: "one", value: "1" },
        { key: "", value: "" },
      ],
    });
  });

  it("convert save values", () => {
    const given = {
      name: "client",
      attributes: [{ key: "one", value: "1" }],
      config: { one: { two: "3" } },
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
        display: { on: { consent: { screen: "true" } } },
        include: { in: { token: { scope: "true" } } },
        gui: { order: "1" },
        consent: { screen: { text: "" } },
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
      attributes: {
        display: { on: { consent: { screen: "true" } } },
        include: { in: { token: { scope: "true" } } },
        gui: { order: "1" },
        consent: { screen: { text: "" } },
      },
    });
  });

  it("convert arrays to form values", () => {
    const given = {
      name: "test",
      description: "",
      redirectUris: ["http://bla.nl", "http://test.nl/bla", "http://test.nl"],
    };
    const values: { [index: string]: any } = {};
    const spy = (name: string, value: any) => (values[name] = value);

    //when
    convertToFormValues(given, spy, ["redirectUris"]);

    //then
    expect(values).toEqual({
      name: "test",
      description: "",
      redirectUris: [
        { value: "http://bla.nl" },
        { value: "http://test.nl/bla" },
        { value: "http://test.nl" },
      ],
    });
  });

  it("convert form values to object", () => {
    const given = {
      redirectUris: [{ value: "http://bla.nl" }, { value: "http://test.nl" }],
    };

    //when
    const values = convertFormValuesToObject(given, ["redirectUris"]);

    //then
    expect(values).toEqual({
      redirectUris: ["http://bla.nl", "http://test.nl"],
    });
  });
});
