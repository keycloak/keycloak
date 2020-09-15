import React, { useState, ReactElement } from "react";
import { AlertType, AlertPanel } from "./AlertPanel";
import { AlertVariant } from "@patternfly/react-core";

export function useAlerts(): [
  (message: string, type?: AlertVariant) => void,
  () => ReactElement,
  (key: number) => void,
  AlertType[]
] {
  const [alerts, setAlerts] = useState<AlertType[]>([]);
  const createId = () => new Date().getTime();

  const hideAlert = (key: number) => {
    setAlerts((alerts) => [...alerts.filter((el) => el.key !== key)]);
  };

  const add = (
    message: string,
    variant: AlertVariant = AlertVariant.default
  ) => {
    const key = createId();
    setAlerts([...alerts, { key, message, variant }]);
    setTimeout(() => hideAlert(key), 8000);
  };

  const Panel = () => <AlertPanel alerts={alerts} onCloseAlert={hideAlert} />;

  return [add, Panel, hideAlert, alerts];
}
