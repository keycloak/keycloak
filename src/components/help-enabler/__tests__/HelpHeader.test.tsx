import React, { useContext } from "react";
import { render, act, fireEvent } from "@testing-library/react";
import { HelpHeader, HelpContext } from "../HelpHeader";

describe("<HelpHeader />", () => {
  it("render", () => {
    const comp = render(<HelpHeader />);
    expect(comp.asFragment()).toMatchSnapshot();
  });

  it("open dropdown", () => {
    const comp = render(<HelpHeader />);
    const button = document.querySelector("button");
    act(() => {
      fireEvent.click(button!);
    });
    expect(comp.asFragment()).toMatchSnapshot();
  });

  it("enable help", () => {
    const HelpEnabled = () => {
      const { enabled } = useContext(HelpContext);
      return <div id="result">{enabled ? "YES" : "NO"}</div>;
    };
    const comp = render(
      <>
        <HelpHeader />
        <HelpEnabled />
      </>
    );

    const button = document.querySelector("button");
    act(() => {
      fireEvent.click(button!);
    });

    const switchComp = document.querySelector("span.pf-c-switch__toggle");
    act(() => {
      fireEvent.click(switchComp!);
    });

    expect(comp.asFragment()).toMatchSnapshot();
    expect(document.getElementById("result")?.innerHTML).toBe("YES");
  });
});
