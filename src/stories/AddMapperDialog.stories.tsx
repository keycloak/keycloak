import React, { useState } from "react";
import { Button } from "@patternfly/react-core";
import type { Meta, Story } from "@storybook/react";
import type { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

import serverInfo from "../context/server-info/__tests__/mock.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";
import {
  AddMapperDialog,
  AddMapperDialogProps,
} from "../client-scopes/add/MapperDialog";

export default {
  title: "Add mapper dialog",
  component: AddMapperDialog,
} as Meta;

const Template: Story<AddMapperDialogProps> = (args) => {
  const [open, setOpen] = useState(false);
  return (
    <ServerInfoContext.Provider
      value={(serverInfo as unknown) as ServerInfoRepresentation}
    >
      <AddMapperDialog
        {...args}
        open={open}
        toggleDialog={() => setOpen(!open)}
      />
      <Button onClick={() => setOpen(true)}>Show</Button>
    </ServerInfoContext.Provider>
  );
};

export const BuildInDialog = Template.bind({});
BuildInDialog.args = {
  protocol: "openid-connect",
  filter: [],
};

export const ProtocolMapperDialog = Template.bind({});
ProtocolMapperDialog.args = {
  protocol: "openid-connect",
};
