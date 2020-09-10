import React from "react";
import { mount } from "enzyme";
import { act } from "@testing-library/react";

import { RealmSelector } from "../RealmSelector";

it("renders realm selector", async () => {
  const wrapper = mount(
    <RealmSelector realm="test" realmList={[{ id: "321", realm: "another" }]} />
  );

  expect(wrapper.text()).toBe("test");

  const expandButton = wrapper.find("button");
  act(() => {
    expandButton!.simulate("click");
  });

  expect(wrapper.html()).toMatchSnapshot();
});
