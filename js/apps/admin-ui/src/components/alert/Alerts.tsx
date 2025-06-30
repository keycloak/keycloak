import { NetworkError } from "@keycloak/keycloak-admin-client";
import { AlertVariant } from "@patternfly/react-core";
import { PropsWithChildren, useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { generateId } from "../../util";

import {
  createNamedContext,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import useSetTimeout from "../../utils/useSetTimeout";
import { AlertPanel } from "./AlertPanel";

const ALERT_TIMEOUT = 8000;

export type AddAlertFunction = (
  message: string,
  variant?: AlertVariant,
  description?: string,
) => void;

export type AddErrorFunction = (message: string, error: unknown) => void;

export type AlertProps = {
  addAlert: AddAlertFunction;
  addError: AddErrorFunction;
};

export const AlertContext = createNamedContext<AlertProps | undefined>(
  "AlertContext",
  undefined,
);

export const useAlerts = () => useRequiredContext(AlertContext);

export type AlertEntry = {
  id: number;
  message: string;
  variant: AlertVariant;
  description?: string;
};

export const AlertProvider = ({ children }: PropsWithChildren) => {
  const { t } = useTranslation();
  const setTimeout = useSetTimeout();
  const [alerts, setAlerts] = useState<AlertEntry[]>([]);

  const removeAlert = (id: number) =>
    setAlerts((alerts) => alerts.filter((alert) => alert.id !== id));

  const addAlert = useCallback<AddAlertFunction>(
    (message, variant = AlertVariant.success, description) => {
      const alert: AlertEntry = {
        id: generateId(),
        message,
        variant,
        description,
      };

      setAlerts((alerts) => [alert, ...alerts]);
      setTimeout(() => removeAlert(alert.id), ALERT_TIMEOUT);
    },
    [],
  );

  const addError = useCallback<AddErrorFunction>((message, error) => {
    addAlert(
      t(message, {
        error: getErrorMessage(error),
      }),
      AlertVariant.danger,
    );
  }, []);

  const value = useMemo(() => ({ addAlert, addError }), []);

  return (
    <AlertContext.Provider value={value}>
      <AlertPanel alerts={alerts} onCloseAlert={removeAlert} />
      {children}
    </AlertContext.Provider>
  );
};

function getErrorMessage(error: unknown) {
  if (typeof error === "string") {
    return error;
  }

  if (error instanceof NetworkError) {
    return getNetworkErrorMessage(error);
  }

  if (error instanceof Error) {
    return error.message;
  }

  throw new Error("Unable to determine error message.");
}

function getNetworkErrorMessage({ responseData }: NetworkError) {
  const data = responseData as Record<string, unknown>;

  for (const key of ["error_description", "errorMessage", "error"]) {
    const value = data[key];

    if (typeof value === "string") {
      return value;
    }
  }
}
