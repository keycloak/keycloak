import {
  AlertGroup,
  Alert,
  AlertActionCloseButton,
  AlertVariant,
} from "@patternfly/react-core";
import type { AlertType } from "./Alerts";

type AlertPanelProps = {
  alerts: AlertType[];
  onCloseAlert: (id: number) => void;
};

export function AlertPanel({ alerts, onCloseAlert }: AlertPanelProps) {
  return (
    <AlertGroup data-testid="global-alerts" isToast>
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
              onClose={() => onCloseAlert(id)}
            />
          }
          timeout
          onTimeout={() => onCloseAlert(id)}
        >
          {description && <p>{description}</p>}
        </Alert>
      ))}
    </AlertGroup>
  );
}
