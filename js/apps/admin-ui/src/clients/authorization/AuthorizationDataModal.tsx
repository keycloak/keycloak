import type AccessTokenRepresentation from "@keycloak/keycloak-admin-client/lib/defs/accessTokenRepresentation";
import {
  Button,
  Modal,
  ModalVariant,
  Text,
  TextArea,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { prettyPrintJSON } from "../../util";
import useToggle from "../../utils/useToggle";

type AuthorizationDataModalProps = {
  data: AccessTokenRepresentation;
};

export const AuthorizationDataModal = ({
  data,
}: AuthorizationDataModalProps) => {
  const { t } = useTranslation();
  const [show, toggle] = useToggle();

  return (
    <>
      <Button
        data-testid="authorization-revert"
        onClick={toggle}
        variant="secondary"
      >
        {t("showAuthData")}
      </Button>
      <Modal
        variant={ModalVariant.medium}
        isOpen={show}
        aria-label={t("authData")}
        header={
          <TextContent>
            <Text component={TextVariants.h1}>{t("authData")}</Text>
            <Text>{t("authDataDescription")}</Text>
          </TextContent>
        }
        onClose={toggle}
        actions={[
          <Button
            data-testid="cancel"
            id="modal-cancel"
            key="cancel"
            onClick={toggle}
          >
            {t("cancel")}
          </Button>,
        ]}
      >
        <TextArea readOnly rows={20} value={prettyPrintJSON(data)} />
      </Modal>
    </>
  );
};
