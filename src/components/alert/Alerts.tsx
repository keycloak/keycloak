import { useState } from "react";
import { AlertType } from "./AlertPanel";
import { AlertVariant } from "@patternfly/react-core";

export function useAlerts(): [
  (message: string, type: AlertVariant) => void,
  AlertType[],
  (key: number) => void
] {
  const [alerts, setAlerts] = useState<AlertType[]>([]);
  const createId = () => new Date().getTime();

  const hideAlert = (key: number) => {
    setAlerts((alerts) => [...alerts.filter((el) => el.key !== key)]);
  };

  const add = (message: string, variant: AlertVariant) => {
    const key = createId();
    setAlerts([...alerts, { key, message, variant }]);
    setTimeout(() => hideAlert(key), 8000);
  };

  return [add, alerts, hideAlert];
}
