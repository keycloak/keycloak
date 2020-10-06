import React from "react";
import { mount } from "enzyme";
import { act } from "@testing-library/react";

import { RealmSelector } from "../RealmSelector";
import { RealmContextProvider } from "../../../context/realm-context/RealmContext";

it("renders realm selector", async () => {
  const wrapper = mount(
    <RealmContextProvider>
      <RealmSelector realmList={[{ id: "321", realm: "another" }]} />
    </RealmContextProvider>
  );

  expect(wrapper.text()).toBe("Master");

  const expandButton = wrapper.find("button");
  act(() => {
    expandButton!.simulate("click");
  });

  expect(wrapper).toMatchSnapshot();
});
