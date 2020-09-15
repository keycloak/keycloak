import React from "react";
import {
  AlertGroup,
  Alert,
  AlertActionCloseButton,
  AlertVariant,
} from "@patternfly/react-core";

export type AlertType = {
  key: number;
  message: string;
  variant: AlertVariant;
};

type AlertPanelProps = {
  alerts: AlertType[];
  onCloseAlert: (key: number) => void;
};

export function AlertPanel({ alerts, onCloseAlert }: AlertPanelProps) {
  return (
    <AlertGroup isToast>
      {alerts.map(({ key, variant, message }) => (
        <Alert
          key={key}
          isLiveRegion
          variant={AlertVariant[variant]}
          variantLabel=""
          title={message}
          actionClose={
            <AlertActionCloseButton
              title={message}
              onClose={() => onCloseAlert(key)}
            />
          }
        />
      ))}
    </AlertGroup>
  );
}
