import React, { useState } from "react";
import { Button } from "@patternfly/react-core";
import { Meta, Story } from "@storybook/react";

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
    <ServerInfoContext.Provider value={serverInfo}>
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
