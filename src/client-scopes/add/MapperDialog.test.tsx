import { Button } from "@patternfly/react-core";
import { fireEvent, render, screen } from "@testing-library/react";
import type { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";
import React, { useState } from "react";
import { ServerInfoContext } from "../../context/server-info/ServerInfoProvider";
import serverInfo from "../../context/server-info/__tests__/mock.json";
import { AddMapperDialog, AddMapperDialogModalProps } from "./MapperDialog";

describe("MapperDialog", () => {
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
        <Button onClick={() => setOpen(true)}>{!open ? "Show" : "Hide"}</Button>
      </ServerInfoContext.Provider>
    );
  };

  it("disables the add button when nothing is selected", () => {
    render(<Test filter={[]} protocol="openid-connect" onConfirm={() => {}} />);

    fireEvent.click(screen.getByText("Show"));

    expect(screen.getByTestId("modalConfirm")).toHaveClass("pf-m-disabled");
  });

  it("returns array with selected build in protocol mapping", () => {
    const onConfirm = jest.fn();
    const protocol = "openid-connect";

    render(<Test filter={[]} protocol={protocol} onConfirm={onConfirm} />);

    fireEvent.click(screen.getByText("Show"));
    fireEvent.click(screen.getByLabelText("Select row 0"));
    fireEvent.click(screen.getByLabelText("Select row 1"));
    fireEvent.click(screen.getByTestId("modalConfirm"));

    expect(onConfirm).toBeCalledWith([
      serverInfo.builtinProtocolMappers[protocol][0],
      serverInfo.builtinProtocolMappers[protocol][1],
    ]);
  });

  it("returns selected protocol mapping type on click", () => {
    const onConfirm = jest.fn();
    const protocol = "openid-connect";

    render(<Test protocol={protocol} onConfirm={onConfirm} />);

    fireEvent.click(screen.getByText("Show"));
    fireEvent.click(screen.getByLabelText("User Realm Role"));

    expect(onConfirm).toBeCalledWith(
      serverInfo.protocolMapperTypes[protocol][0]
    );
  });

  it("closes the dialog on 'X' click", () => {
    render(<Test protocol="openid-connect" onConfirm={() => {}} />);

    fireEvent.click(screen.getByText("Show"));
    expect(screen.getByText("Hide")).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));
    expect(screen.getByText("Show")).toBeInTheDocument();
  });
});
