import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  AlertVariant,
  PageSection,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { type FallbackProps } from "../../context/ErrorBoundary";

export const ErrorRenderer = ({ error }: FallbackProps) => {
  const { t } = useTranslation();

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
          <AlertActionLink onClick={reset}>{t("retry")}</AlertActionLink>
        }
      ></Alert>
    </PageSection>
  );
};
