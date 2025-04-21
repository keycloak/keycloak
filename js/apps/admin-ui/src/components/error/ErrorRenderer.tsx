import { NetworkError } from "@keycloak/keycloak-admin-client";
import { type FallbackProps } from "@keycloak/keycloak-ui-shared";
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

  const reset = () => {
    window.location.href = window.location.origin + window.location.pathname;
  };

  let message;
  if (error instanceof NetworkError && error.response.status === 403) {
    message = t("forbiddenAdminConsole");
  } else {
    message = error.message;
  }
  return (
    <PageSection>
      <Alert
        isInline
        variant={AlertVariant.danger}
        title={message}
        actionClose={
          <AlertActionCloseButton title={error.message} onClose={reset} />
        }
        actionLinks={
          <AlertActionLink onClick={() => location.reload()}>
            {t("reload")}
          </AlertActionLink>
        }
      ></Alert>
    </PageSection>
  );
};
