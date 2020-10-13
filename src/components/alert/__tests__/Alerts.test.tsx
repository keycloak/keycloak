import React from "react";
import { Button } from "@patternfly/react-core";
import { mount } from "enzyme";
import { act } from "react-dom/test-utils";

import { AlertPanel } from "../AlertPanel";
import { AlertProvider, useAlerts } from "../Alerts";

jest.useFakeTimers();

const WithButton = () => {
  const { addAlert } = useAlerts();
  return (
    <AlertProvider>
      <Button onClick={() => addAlert("Hello")}>Add</Button>
    </AlertProvider>
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
