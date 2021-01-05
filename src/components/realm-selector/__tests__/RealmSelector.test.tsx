import React from "react";
import { mount } from "enzyme";
import { act } from "@testing-library/react";

import { RealmSelector } from "../RealmSelector";
import { RealmContextProvider } from "../../../context/realm-context/RealmContext";
import { MemoryRouter } from "react-router-dom";

it("renders realm selector", async () => {
  const wrapper = mount(
    <MemoryRouter>
      <RealmContextProvider>
        <div id="realm">
          <RealmSelector realmList={[{ id: "321", realm: "another" }]} />
        </div>
      </RealmContextProvider>
    </MemoryRouter>
  );

  const expandButton = wrapper.find("button");
  act(() => {
    expandButton!.simulate("click");
  });

  expect(wrapper.find("#realm")).toMatchSnapshot();
});
