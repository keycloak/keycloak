import React from "react";
import { Button } from "@patternfly/react-core";
import { mount } from "enzyme";
import { act } from "react-dom/test-utils";

import { AlertPanel } from "../AlertPanel";
import { useAlerts } from "../Alerts";

jest.useFakeTimers();

const WithButton = () => {
  const [add, _, hide, alerts] = useAlerts();
  return (
    <>
      <AlertPanel alerts={alerts} onCloseAlert={hide} />
      <Button onClick={() => add("Hello")}>Add</Button>
    </>
  );
};

it("renders empty alert panel", () => {
  const empty = mount(<AlertPanel alerts={[]} onCloseAlert={() => {}} />);
  expect(empty).toMatchSnapshot();
});

it("remove alert after timeout", () => {
  const tree = mount(<WithButton />);
  const button = tree.find("button");
  expect(button).not.toBeNull();

  act(() => {
    button!.simulate("click");
  });
  expect(tree).toMatchSnapshot("with alert");

  act(() => {
    jest.runAllTimers();
  });
  expect(tree).toMatchSnapshot("cleared alert");
});
