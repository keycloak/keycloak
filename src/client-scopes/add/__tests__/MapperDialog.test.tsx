import React, { useState } from "react";
import { mount } from "enzyme";
import { Button } from "@patternfly/react-core";
import type { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

import serverInfo from "../../../context/server-info/__tests__/mock.json";
import { ServerInfoContext } from "../../../context/server-info/ServerInfoProvider";
import { AddMapperDialogModalProps, AddMapperDialog } from "../MapperDialog";

describe("<MapperDialog/>", () => {
  const Test = (args: AddMapperDialogModalProps) => {
    const [open, setOpen] = useState(false);
    return (
      <ServerInfoContext.Provider
        value={serverInfo as unknown as ServerInfoRepresentation}
      >
        <AddMapperDialog
          {...args}
          open={open}
          toggleDialog={() => setOpen(!open)}
        />
        <Button id="open" onClick={() => setOpen(true)}>
          {!open ? "Show" : "Hide"}
        </Button>
      </ServerInfoContext.Provider>
    );
  };

  it("should have disabled add button when nothing is selected", () => {
    const container = mount(
      <Test filter={[]} protocol="openid-connect" onConfirm={() => {}} />
    );

    container.find("button#open").simulate("click");
    expect(container).toMatchSnapshot();

    const button = container.find("button#modal-confirm");
    expect(button.hasClass("pf-m-disabled")).toBe(true);
  });

  it("should return array with selected build in protocol mapping", () => {
    const onConfirm = jest.fn();
    const protocol = "openid-connect";
    const container = mount(
      <Test filter={[]} protocol={protocol} onConfirm={onConfirm} />
    );

    container.find("button#open").simulate("click");
    container
      .find("input[name='checkrow0']")
      .simulate("change", { target: { value: true } });
    container
      .find("input[name='checkrow1']")
      .simulate("change", { target: { value: true } });

    container.find("button#modal-confirm").simulate("click");
    expect(onConfirm).toBeCalledWith([
      serverInfo.builtinProtocolMappers[protocol][0],
      serverInfo.builtinProtocolMappers[protocol][1],
    ]);
  });

  it("should return selected protocol mapping type on click", () => {
    const onConfirm = jest.fn();
    const protocol = "openid-connect";
    const container = mount(<Test protocol={protocol} onConfirm={onConfirm} />);

    container.find("button#open").simulate("click");
    expect(container).toMatchSnapshot();

    container
      .find("div.pf-c-data-list__item-content")
      .first()
      .simulate("click");
    expect(onConfirm).toBeCalledWith(
      serverInfo.protocolMapperTypes[protocol][0]
    );
  });

  it("should close the dialog on 'X' click", () => {
    const container = mount(
      <Test protocol="openid-connect" onConfirm={() => {}} />
    );

    expect(container.find("button#open").text()).toBe("Show");
    container.find("button#open").simulate("click");
    expect(container.find("button#open").text()).toBe("Hide");
    container.find('button[aria-label="Close"]').simulate("click");

    expect(container.find("button#open").text()).toBe("Show");
  });
});
