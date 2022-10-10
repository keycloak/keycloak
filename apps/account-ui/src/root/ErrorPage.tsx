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
import { useRouteError } from "react-router";

export const ErrorPage = () => {
  const { t } = useTranslation();
  const error = useRouteError();
  const errorMessage = getErrorMessage(error);

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
          {errorMessage && (
            <Text component={TextVariants.small}>{errorMessage}</Text>
          )}
        </TextContent>
      </Modal>
    </Page>
  );
};

function getErrorMessage(error: unknown) {
  if (typeof error === "string") {
    return error;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return null;
}
