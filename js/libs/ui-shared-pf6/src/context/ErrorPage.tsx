import {
  Button,
  Content,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Page,
} from "@patternfly/react-core";
import { Modal, ModalVariant } from "@patternfly/react-core/deprecated";
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
      <Modal variant={ModalVariant.small} isOpen>
        <ModalHeader
          title={t("somethingWentWrong")}
          titleIconVariant="danger"
        />
        <ModalBody>
          <Content>
            {errorMessage ? (
              <p>{t(errorMessage)}</p>
            ) : networkErrorMessage && i18n.exists(networkErrorMessage) ? (
              <p>{t(networkErrorMessage)}</p>
            ) : (
              <p>{t("somethingWentWrongDescription")}</p>
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
