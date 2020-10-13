import React from "react";
import { AlertVariant, Button } from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { AlertPanel } from "../components/alert/AlertPanel";
import { AlertProvider, useAlerts } from "../components/alert/Alerts";

export default {
  title: "Alert Panel",
  component: AlertPanel,
} as Meta;

export const Api = () => (
  <AlertPanel
    alerts={[{ key: 1, message: "Hello", variant: AlertVariant.default }]}
    onCloseAlert={() => {}}
  />
);
export const AddAlert = () => {
  const { addAlert } = useAlerts();
  return (
    <AlertProvider>
      <Button onClick={() => addAlert("Hello", AlertVariant.default)}>
        Add
      </Button>
    </AlertProvider>
  );
};
