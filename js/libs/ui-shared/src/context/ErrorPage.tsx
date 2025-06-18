import {
  Button,
  Modal,
  ModalVariant,
  Page,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { getNetworkErrorDescription } from "../utils/errors";

type ErrorPageProps = {
  error?: unknown;
};

export const ErrorPage = (props: ErrorPageProps) => {
  const { t } = useTranslation();
  const error = props.error;
  const errorMessage =
    getErrorMessage(error) ||
    getNetworkErrorDescription(error)?.replace(/\+/g, " ");
  console.error(error);

  function onRetry() {
    location.href = location.origin + location.pathname;
  }

  return (
    <Page>
      <Modal
        variant={ModalVariant.small}
        title={errorMessage ? "" : t("somethingWentWrong")}
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
          ) : (
            <Text>{t("somethingWentWrongDescription")}</Text>
          )}
        </TextContent>
      </Modal>
    </Page>
  );
};

function getErrorMessage(error: unknown): string | null {
  if (typeof error === "string") {
    return error;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return null;
}
