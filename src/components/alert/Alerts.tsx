import React, { createContext, FunctionComponent, useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant } from "@patternfly/react-core";
import type { AxiosError } from "axios";

import useRequiredContext from "../../utils/useRequiredContext";
import { AlertPanel, AlertType } from "./AlertPanel";

type AlertProps = {
  addAlert: (
    message: string,
    variant?: AlertVariant,
    description?: string
  ) => void;

  addError: (message: string, error: any) => void;
};

export const AlertContext = createContext<AlertProps | undefined>(undefined);

export const useAlerts = () => useRequiredContext(AlertContext);

export const AlertProvider: FunctionComponent = ({ children }) => {
  const { t } = useTranslation();
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

  const addError = (message: string, error: Error | AxiosError) => {
    addAlert(
      t(message, {
        error:
          "response" in error
            ? error.response?.data?.errorMessage || error.response?.data?.error
            : error,
      }),
      AlertVariant.danger
    );
  };

  return (
    <AlertContext.Provider value={{ addAlert, addError }}>
      <AlertPanel alerts={alerts} onCloseAlert={hideAlert} />
      {children}
    </AlertContext.Provider>
  );
};
