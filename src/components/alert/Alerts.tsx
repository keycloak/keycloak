import { AlertVariant } from "@patternfly/react-core";
import React, { createContext, ReactNode, useState } from "react";
import useRequiredContext from "../../utils/useRequiredContext";
import { AlertPanel, AlertType } from "./AlertPanel";

type AlertProps = {
  addAlert: (
    message: string,
    variant?: AlertVariant,
    description?: string
  ) => void;
};

export const AlertContext = createContext<AlertProps | undefined>(undefined);

export const useAlerts = () => useRequiredContext(AlertContext);

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
