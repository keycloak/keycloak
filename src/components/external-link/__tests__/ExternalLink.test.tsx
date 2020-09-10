import React from "react";
import { render } from "@testing-library/react";
import { ExternalLink } from "../ExternalLink";

describe("<ExternalLink />", () => {
  it("render with link", () => {
    const comp = render(<ExternalLink href="http://hello.nl/" />);
    expect(comp.asFragment()).toMatchSnapshot();
  });

  it("render with link and title", () => {
    const comp = render(
      <ExternalLink href="http://hello.nl/" title="Link to Hello" />
    );
    expect(comp.asFragment()).toMatchSnapshot();
  });
});
