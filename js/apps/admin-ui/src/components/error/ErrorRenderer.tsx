import {
  useErrorBoundary,
  type FallbackProps,
} from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  AlertVariant,
  PageSection,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

export const ErrorRenderer = ({ error }: FallbackProps) => {
  const { t } = useTranslation();
  const { showBoundary } = useErrorBoundary();

  const reset = () => {
    window.location.href = window.location.origin + window.location.pathname;
  };

  return (
    <PageSection>
      <Alert
        isInline
        variant={AlertVariant.danger}
        title={error.message}
        actionClose={
          <AlertActionCloseButton title={error.message} onClose={reset} />
        }
        actionLinks={
          <AlertActionLink onClick={() => showBoundary()}>
            {t("retry")}
          </AlertActionLink>
        }
      ></Alert>
    </PageSection>
  );
};
