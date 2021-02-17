import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  AlertVariant,
  PageSection,
} from "@patternfly/react-core";
import React from "react";
import { FallbackProps } from "react-error-boundary";
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
          <React.Fragment>
            <AlertActionLink onClick={resetErrorBoundary}>
              {t("retry")}
            </AlertActionLink>
          </React.Fragment>
        }
      ></Alert>
    </PageSection>
  );
};
