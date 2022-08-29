import { FunctionComponent, useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant } from "@patternfly/react-core";
import axios from "axios";
import type { AxiosError } from "axios";

import { createNamedContext } from "../../utils/createNamedContext";
import useRequiredContext from "../../utils/useRequiredContext";
import { AlertPanel } from "./AlertPanel";

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

export const AlertContext = createNamedContext<AlertProps | undefined>(
  "AlertContext",
  undefined
);

export const useAlerts = () => useRequiredContext(AlertContext);

export type AlertType = {
  id: number;
  message: string;
  variant: AlertVariant;
  description?: string;
};

export const AlertProvider: FunctionComponent = ({ children }) => {
  const { t } = useTranslation();
  const [alerts, setAlerts] = useState<AlertType[]>([]);

  const hideAlert = (id: number) => {
    setAlerts((alerts) => alerts.filter((alert) => alert.id !== id));
  };

  const addAlert = (
    message: string,
    variant: AlertVariant = AlertVariant.success,
    description?: string
  ) => {
    setAlerts([
      {
        id: Math.random(),
        message,
        variant,
        description,
      },
      ...alerts,
    ]);
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

  const responseData = (error.response?.data ?? {}) as Record<string, unknown>;

  for (const key of ["error_description", "errorMessage", "error"]) {
    const value = responseData[key];

    if (typeof value === "string") {
      return value;
    }
  }

  return error.message;
}
