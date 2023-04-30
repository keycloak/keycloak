import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  AlertVariant,
  Page,
} from "@patternfly/react-core";

import { useTranslation } from "react-i18next";
import { isRouteErrorResponse, useRouteError } from "react-router-dom";

export const ErrorPage = () => {
  const { t } = useTranslation();
  const error = useRouteError();
  const errorMessage = getErrorMessage(error);

  function onRetry() {
    location.href = location.origin + location.pathname;
  }
  return (
    <Page>
      <Alert
        isInline
        variant={AlertVariant.danger}
        title={errorMessage}
        actionClose={
          <AlertActionCloseButton title={errorMessage} onClose={onRetry} />
        }
        actionLinks={
          <AlertActionLink onClick={onRetry}>{t("retry")}</AlertActionLink>
        }
      ></Alert>
    </Page>
  );
};

function getErrorMessage(error: unknown): string {
  if (typeof error === "string") {
    return error;
  }

  if (isRouteErrorResponse(error)) {
    return error.error ? getErrorMessage(error.error) : "Something went wrong!";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Something went wrong!";
}
