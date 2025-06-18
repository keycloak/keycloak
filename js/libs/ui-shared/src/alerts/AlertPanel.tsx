import {
  AlertGroup,
  Alert,
  AlertActionCloseButton,
  AlertVariant,
} from "@patternfly/react-core";

import type { AlertEntry } from "./Alerts";

export type AlertPanelProps = {
  alerts: AlertEntry[];
  onCloseAlert: (id: number) => void;
};

export function AlertPanel({ alerts, onCloseAlert }: AlertPanelProps) {
  return (
    <AlertGroup
      data-testid="global-alerts"
      isToast
      style={{ whiteSpace: "pre-wrap" }}
    >
      {alerts.map(({ id, variant, message, description }, index) => (
        <Alert
          key={id}
          data-testid={index === 0 ? "last-alert" : undefined}
          isLiveRegion
          variant={AlertVariant[variant]}
          component="p"
          variantLabel=""
          title={message}
          actionClose={
            <AlertActionCloseButton
              title={message}
              onClose={() => onCloseAlert(id)}
            />
          }
        >
          {description && <p>{description}</p>}
        </Alert>
      ))}
    </AlertGroup>
  );
}
