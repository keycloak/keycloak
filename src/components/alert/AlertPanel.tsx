import React from 'react';
import {
  AlertGroup,
  Alert,
  AlertActionCloseButton,
  AlertVariant,
} from '@patternfly/react-core';

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
          isLiveRegion
          variant={AlertVariant[variant]}
          title={message}
          actionClose={
            <AlertActionCloseButton
              title={message}
              variantLabel={`${variant} alert`}
              onClose={() => onCloseAlert(key)}
            />
          }
          key={key}
        />
      ))}
    </AlertGroup>
  );
}
