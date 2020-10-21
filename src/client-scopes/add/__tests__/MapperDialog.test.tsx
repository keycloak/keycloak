import React from "react";
import { mount } from "enzyme";
import { Button } from "@patternfly/react-core";

import serverInfo from "../../../context/server-info/__tests__/mock.json";
import { ServerInfoContext } from "../../../context/server-info/ServerInfoProvider";
import { AddMapperDialogProps, useAddMapperDialog } from "../MapperDialog";

describe("<MapperDialog/>", () => {
  const Test = (args: AddMapperDialogProps) => {
    const [toggle, Dialog] = useAddMapperDialog(args);
    return (
      <ServerInfoContext.Provider value={serverInfo}>
        <Dialog />
        <Button id="open" onClick={toggle}>
          Show
        </Button>
      </ServerInfoContext.Provider>
    );
  };

  it("should return empty array when selecting nothing", () => {
    const onConfirm = jest.fn();
    const container = mount(
      <Test buildIn={true} protocol="openid-connect" onConfirm={onConfirm} />
    );

    container.find("button#open").simulate("click");
    expect(container).toMatchSnapshot();

    container.find("button#modal-confirm").simulate("click");
    expect(onConfirm).toBeCalledWith([]);
  });

  it("should return array with selected build in protocol mapping", () => {
    const onConfirm = jest.fn();
    const protocol = "openid-connect";
    const container = mount(
      <Test buildIn={true} protocol={protocol} onConfirm={onConfirm} />
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
    const container = mount(
      <Test buildIn={false} protocol={protocol} onConfirm={onConfirm} />
    );

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
});
