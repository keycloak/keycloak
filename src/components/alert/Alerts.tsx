import React, { createContext, FunctionComponent, useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant } from "@patternfly/react-core";
import axios from "axios";
import type { AxiosError } from "axios";

import useRequiredContext from "../../utils/useRequiredContext";
import useSetTimeout from "../../utils/useSetTimeout";
import { AlertPanel, AlertType } from "./AlertPanel";

export type AddAlertFunction = (
  message: string,
  variant?: AlertVariant,
  description?: string
) => void;

export type AddErrorFunction = (message: string, error: any) => void;

type AlertProps = {
  addAlert: AddAlertFunction;
  addError: AddErrorFunction;
};

export const AlertContext = createContext<AlertProps | undefined>(undefined);

export const useAlerts = () => useRequiredContext(AlertContext);

export const AlertProvider: FunctionComponent = ({ children }) => {
  const { t } = useTranslation();
  const [alerts, setAlerts] = useState<AlertType[]>([]);
  const setTimeout = useSetTimeout();

  const createId = () => Math.random().toString(16);

  const hideAlert = (key: string) => {
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

  const addError = (message: string, error: Error | AxiosError | string) => {
    addAlert(
      t(message, {
        error: getErrorMessage(error),
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

function getErrorMessage(
  error: Error | AxiosError<Record<string, unknown>> | string
) {
  if (typeof error === "string") {
    return error;
  }

  if (!axios.isAxiosError(error)) {
    return error.message;
  }

  const responseData = error.response?.data ?? {};

  for (const key of ["error_description", "errorMessage", "error"]) {
    const value = responseData[key];

    if (typeof value === "string") {
      return value;
    }
  }

  return error.message;
}
