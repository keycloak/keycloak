import { useTranslation } from "react-i18next";
import {
  Alert,
  AlertVariant,
  ClipboardCopy,
  Form,
  FormGroup,
  Modal,
  ModalBody,
  ModalHeader,
  ModalVariant,
} from "@patternfly/react-core";
type AccessTokenDialogProps = {
  token: string;
  toggleDialog: () => void;
};

export const AccessTokenDialog = ({
  token,
  toggleDialog,
}: AccessTokenDialogProps) => {
  const { t } = useTranslation();
  return (
    <Modal
      isOpen={true}
      onClose={toggleDialog}
      variant={ModalVariant.medium}
      aria-label={t("initialAccessTokenDetails")}
    >
      <ModalHeader title={t("initialAccessTokenDetails")} />
      <ModalBody>
        <Alert
          title={t("copyInitialAccessToken")}
          component="h2"
          isInline
          variant={AlertVariant.warning}
        />
        <Form className="pf-v6-u-mt-md">
          <FormGroup
            label={t("initialAccessToken")}
            fieldId="initialAccessToken"
          >
            <ClipboardCopy
              id="initialAccessToken"
              data-testid="initialAccessToken"
              isReadOnly
            >
              {token}
            </ClipboardCopy>
          </FormGroup>
        </Form>
      </ModalBody>
    </Modal>
  );
};
