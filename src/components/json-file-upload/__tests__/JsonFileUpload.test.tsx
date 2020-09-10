import React from "react";
import { mount } from "enzyme";

import { JsonFileUpload } from "../JsonFileUpload";

describe("<JsonFileUpload />", () => {
  it("render", () => {
    const comp = mount(<JsonFileUpload id="test" onChange={jest.fn()} />);
    expect(comp).toMatchSnapshot();
  });

  it("upload file", async () => {
    const onChange = jest.fn((value) => value);
    const comp = mount(<JsonFileUpload id="upload" onChange={onChange} />);

    const fileInput = comp.find('[type="file"]');
    expect(fileInput.length).toBe(1);

    const json = '{"bla": "test"}';
    const file = new File([json], "test.json");

    const dummyFileReader = {
      onload: jest.fn(),
      readAsText: () => Promise.resolve(json),
    };
    (window as any).FileReader = jest.fn(() => dummyFileReader);

    fileInput.simulate("change", {
      target: {
        files: [file],
      },
    });
    expect(comp).toMatchSnapshot();
  });
});
