import {
  Button,
  Modal,
  ModalVariant,
  Page,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { isRouteErrorResponse, useRouteError } from "react-router-dom";
import { getErrorMessage as getBaseErrorMessage } from "ui-shared";

type ErrorPageProps = {
  error?: unknown;
};

export const ErrorPage = (props: ErrorPageProps) => {
  const { t } = useTranslation();
  const error = useRouteError() ?? props.error;
  const errorMessage = getErrorMessage(error) ?? t("unknownError");

  function onRetry() {
    location.href = location.origin + location.pathname;
  }

  return (
    <Page>
      <Modal
        variant={ModalVariant.small}
        title={t("somethingWentWrong")}
        titleIconVariant="danger"
        showClose={false}
        isOpen
        actions={[
          <Button key="tryAgain" variant="primary" onClick={onRetry}>
            {t("tryAgain")}
          </Button>,
        ]}
      >
        <TextContent>
          <Text>{t("somethingWentWrongDescription")}</Text>
          <Text component={TextVariants.small}>{errorMessage}</Text>
        </TextContent>
      </Modal>
    </Page>
  );
};

function getErrorMessage(error: unknown) {
  if (isRouteErrorResponse(error)) {
    return error.statusText;
  }

  return getBaseErrorMessage(error);
}
