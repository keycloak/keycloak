import React from "react";
import { render } from "@testing-library/react";
import { FormattedLink } from "../FormattedLink";

describe("<ExternalLink />", () => {
  it("render with link", () => {
    const comp = render(<FormattedLink href="http://hello.nl/" />);
    expect(comp.asFragment()).toMatchSnapshot();
  });

  it("render with link and title", () => {
    const comp = render(
      <FormattedLink href="http://hello.nl/" title="Link to Hello" />
    );
    expect(comp.asFragment()).toMatchSnapshot();
  });

  it("render with internal url", () => {
    const comp = render(
      <FormattedLink href="/application/home/" title="Application page" />
    );
    expect(comp.asFragment()).toMatchSnapshot();
  });

  it("render as application", () => {
    const comp = render(
      <FormattedLink href="/application/main" title="Application link" />
    );
    expect(comp.asFragment()).toMatchSnapshot();
  });
});
