import { AlertVariant } from "@patternfly/react-core";
import { PropsWithChildren, useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import { createNamedContext } from "../utils/createNamedContext";
import { getErrorDescription, getErrorMessage } from "../utils/errors";
import { generateId } from "../utils/generateId";
import { useRequiredContext } from "../utils/useRequiredContext";
import { useSetTimeout } from "../utils/useSetTimeout";
import { AlertPanel } from "./AlertPanel";

const ALERT_TIMEOUT = 8000;

export type AddAlertFunction = (
  message: string,
  variant?: AlertVariant,
  description?: string,
) => void;

export type AddErrorFunction = (messageKey: string, error: unknown) => void;

export type AlertProps = {
  addAlert: AddAlertFunction;
  addError: AddErrorFunction;
};

const AlertContext = createNamedContext<AlertProps | undefined>(
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
    [setTimeout],
  );

  const addError = useCallback<AddErrorFunction>(
    (messageKey, error) => {
      const message = t(messageKey, { error: getErrorMessage(error) });
      const description = getErrorDescription(error);

      addAlert(message, AlertVariant.danger, description);
    },
    [addAlert, t],
  );

  const value = useMemo(() => ({ addAlert, addError }), [addAlert, addError]);

  return (
    <AlertContext.Provider value={value}>
      <AlertPanel alerts={alerts} onCloseAlert={removeAlert} />
      {children}
    </AlertContext.Provider>
  );
};
