import React from "react";
import { AlertVariant, Button } from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { AlertPanel } from "../components/alert/AlertPanel";
import { useAlerts } from "../components/alert/Alerts";

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
  const [add, alerts, hide] = useAlerts();
  return (
    <>
      <AlertPanel alerts={alerts} onCloseAlert={hide} />
      <Button onClick={() => add("Hello", AlertVariant.default)}>Add</Button>
    </>
  );
};
