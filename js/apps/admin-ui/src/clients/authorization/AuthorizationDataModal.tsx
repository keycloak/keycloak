import type AccessTokenRepresentation from "@keycloak/keycloak-admin-client/lib/defs/accessTokenRepresentation";
import {
  Button,
  Content,
  ContentVariants,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
  TextArea,
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
        onClose={toggle}
      >
        <ModalHeader>
          <Content>
            <Content component={ContentVariants.h1}>{t("authData")}</Content>
            <Content component="p">{t("authDataDescription")}</Content>
          </Content>
        </ModalHeader>
        <ModalBody>
          <TextArea readOnly rows={20} value={prettyPrintJSON(data)} />
        </ModalBody>
        <ModalFooter>
          <Button
            data-testid="cancel"
            id="modal-cancel"
            key="cancel"
            onClick={toggle}
          >
            {t("cancel")}
          </Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
