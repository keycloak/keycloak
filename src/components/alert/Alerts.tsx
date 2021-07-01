import React, { useState, createContext, ReactNode, useContext } from "react";
import { AlertType, AlertPanel } from "./AlertPanel";
import { AlertVariant } from "@patternfly/react-core";

type AlertProps = {
  addAlert: (
    message: string,
    variant?: AlertVariant,
    description?: string
  ) => void;
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
    variant: AlertVariant = AlertVariant.success,
    description?: string
  ) => {
    const key = createId();
    setTimeout(() => hideAlert(key), 8000);
    setAlerts([{ key, message, variant, description }, ...alerts]);
  };

  return (
    <AlertContext.Provider value={{ addAlert }}>
      <AlertPanel alerts={alerts} onCloseAlert={hideAlert} />
      {children}
    </AlertContext.Provider>
  );
};
