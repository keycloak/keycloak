import React from "react";
import { render } from "@testing-library/react";
import { ListEmptyState } from "../ListEmptyState";

describe("<ListEmptyState />", () => {
  it("render", () => {
    const comp = render(
      <ListEmptyState
        message="No things"
        instructions="You haven't created any things for this list."
        primaryActionText="Add a thing"
        onPrimaryAction={() => {}}
      />
    );
    expect(comp.asFragment()).toMatchSnapshot();
  });
});
