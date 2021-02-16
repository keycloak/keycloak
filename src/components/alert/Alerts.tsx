import React, { useState, createContext, ReactNode, useContext } from "react";
import { AlertType, AlertPanel } from "./AlertPanel";
import { AlertVariant } from "@patternfly/react-core";

type AlertProps = {
  addAlert: (message: string, variant?: AlertVariant) => void;
};

export const AlertContext = createContext<AlertProps>({
  addAlert: () => {},
});

export const useAlerts = () => useContext(AlertContext);

type TimeOut = {
  key: number;
  timeOut: NodeJS.Timeout;
};

export const AlertProvider = ({ children }: { children: ReactNode }) => {
  const [alerts, setAlerts] = useState<AlertType[]>([]);

  const createId = () => new Date().getTime();

  const hideAlert = (key: number) => {
    setAlerts((alerts) => [...alerts.filter((el) => el.key !== key)]);
  };

  const addAlert = (
    message: string,
    variant: AlertVariant = AlertVariant.default
  ) => {
    setAlerts([...alerts, { key: createId(), message, variant }]);
  };

  return (
    <AlertContext.Provider value={{ addAlert }}>
      <AlertPanel alerts={alerts} onCloseAlert={hideAlert} />
      {children}
    </AlertContext.Provider>
  );
};
