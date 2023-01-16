import { createContext, FunctionComponent, useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Alert,
  AlertActionCloseButton,
  AlertGroup,
  AlertVariant,
} from "@patternfly/react-core";
import { TranslationKeys } from "../../react-i18next";

export type AddAlertFunction = (
  message: TranslationKeys,
  variant?: AlertVariant,
  description?: string
) => void;

export type AddErrorFunction = (message: TranslationKeys, error: any) => void;

type AlertProps = {
  addAlert: AddAlertFunction;
  addError: AddErrorFunction;
};

export const AlertContext = createContext<AlertProps | undefined>(undefined);

export const useAlerts = () => useContext(AlertContext)!;

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
    message: TranslationKeys,
    variant: AlertVariant = AlertVariant.success,
    description?: string
  ) => {
    setAlerts([
      {
        id: Math.random() * 100,
        //@ts-ignore
        message: t(message),
        variant,
        description,
      },
      ...alerts,
    ]);
  };

  const addError = (message: TranslationKeys, error: Error | string) => {
    addAlert(
      //@ts-ignore
      t(message, {
        error,
      }),
      AlertVariant.danger
    );
  };

  return (
    <AlertContext.Provider value={{ addAlert, addError }}>
      <AlertGroup isToast>
        {alerts.map(({ id, variant, message, description }) => (
          <Alert
            key={id}
            isLiveRegion
            variant={AlertVariant[variant]}
            variantLabel=""
            title={message}
            actionClose={
              <AlertActionCloseButton
                title={message}
                onClose={() => hideAlert(id)}
              />
            }
            timeout
            onTimeout={() => hideAlert(id)}
          >
            {description && <p>{description}</p>}
          </Alert>
        ))}
      </AlertGroup>
      {children}
    </AlertContext.Provider>
  );
};
