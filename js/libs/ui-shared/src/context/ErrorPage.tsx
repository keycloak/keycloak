import {
  Button,
  Modal,
  ModalVariant,
  Page,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { getNetworkErrorMessage } from "../utils/errors";

type ErrorPageProps = {
  error?: unknown;
};

export const ErrorPage = (props: ErrorPageProps) => {
  const { t, i18n } = useTranslation();
  const error = props.error;
  const errorMessage = getErrorMessage(error);
  const networkErrorMessage = getNetworkErrorMessage(error);
  console.error(error);

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
          {errorMessage ? (
            <Text>{t(errorMessage)}</Text>
          ) : networkErrorMessage && i18n.exists(networkErrorMessage) ? (
            <Text>{t(networkErrorMessage)}</Text>
          ) : (
            <Text>{t("somethingWentWrongDescription")}</Text>
          )}
        </TextContent>
      </Modal>
    </Page>
  );
};

function getErrorMessage(error: unknown): string | null {
  if (error instanceof Error) {
    return error.message;
  }

  return null;
}
