import React from "react";
import { Button } from "@patternfly/react-core";
import { Meta, Story } from "@storybook/react";

import serverInfo from "../context/server-info/__tests__/mock.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";
import {
  AddMapperDialog,
  AddMapperDialogProps,
  useAddMapperDialog,
} from "../client-scopes/add/MapperDialog";

export default {
  title: "Add mapper dialog",
  component: AddMapperDialog,
} as Meta;

const Template: Story<AddMapperDialogProps> = (args) => {
  const [toggle, Dialog] = useAddMapperDialog(args);
  return (
    <ServerInfoContext.Provider value={serverInfo}>
      <Dialog />
      <Button onClick={toggle}>Show</Button>
    </ServerInfoContext.Provider>
  );
};

export const BuildInDialog = Template.bind({});
BuildInDialog.args = {
  protocol: "openid-connect",
  buildIn: true,
};

export const ProtocolMapperDialog = Template.bind({});
ProtocolMapperDialog.args = {
  protocol: "openid-connect",
  buildIn: false,
};
