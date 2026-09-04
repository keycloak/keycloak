import {
  Button,
  Content,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
  Page,
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
        isOpen
        aria-label={t("somethingWentWrong")}
      >
        <ModalHeader
          title={t("somethingWentWrong")}
          titleIconVariant="danger"
        />
        <ModalBody>
          <Content>
            {errorMessage ? (
              <Content component="p">{t(errorMessage)}</Content>
            ) : networkErrorMessage && i18n.exists(networkErrorMessage) ? (
              <Content component="p">{t(networkErrorMessage)}</Content>
            ) : (
              <Content component="p">
                {t("somethingWentWrongDescription")}
              </Content>
            )}
          </Content>
        </ModalBody>
        <ModalFooter>
          <Button key="tryAgain" variant="primary" onClick={onRetry}>
            {t("tryAgain")}
          </Button>
        </ModalFooter>
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
