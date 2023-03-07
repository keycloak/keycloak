import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  AlertVariant,
  PageSection,
} from "@patternfly/react-core";

import type { FallbackProps } from "react-error-boundary";
import { useTranslation } from "react-i18next";

export const ErrorRenderer = ({ error, resetErrorBoundary }: FallbackProps) => {
  const { t } = useTranslation();
  return (
    <PageSection>
      <Alert
        isInline
        variant={AlertVariant.danger}
        title={error.message}
        actionClose={
          <AlertActionCloseButton
            title={error.message}
            onClose={resetErrorBoundary}
          />
        }
        actionLinks={
          <AlertActionLink onClick={resetErrorBoundary}>
            {t("retry")}
          </AlertActionLink>
        }
      ></Alert>
    </PageSection>
  );
};
