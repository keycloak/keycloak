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
    const key = createId();
    setAlerts([...alerts, { key, message, variant }]);
    setTimeout(() => hideAlert(key), 8000);
  };

  return (
    <AlertContext.Provider value={{ addAlert }}>
      <AlertPanel alerts={alerts} onCloseAlert={hideAlert} />
      {children}
    </AlertContext.Provider>
  );
};
