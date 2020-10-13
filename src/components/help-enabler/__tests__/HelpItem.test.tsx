import React from "react";
import { render } from "@testing-library/react";

import { HelpItem } from "../HelpItem";

describe("<HelpItem />", () => {
  it("render", () => {
    const comp = render(
      <HelpItem helpText="storybook" forLabel="storybook" forID="placeholder" />
    );
    expect(comp.asFragment()).toMatchSnapshot();
  });
});
