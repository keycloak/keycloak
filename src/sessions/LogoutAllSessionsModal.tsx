import React from "react";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Modal,
  ModalVariant,
  TextContent,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";

type RevocationModalProps = {
  handleModalToggle: () => void;
};

export const LogoutAllSessionsModal = ({
  handleModalToggle,
}: RevocationModalProps) => {
  const { t } = useTranslation("sessions");
  const { addAlert } = useAlerts();

  const { realm: realmName } = useRealm();
  const adminClient = useAdminClient();

  const logoutAllSessions = async () => {
    try {
      await adminClient.realms.logoutAll({ realm: realmName });
      adminClient.keycloak.logout({ redirectUri: "" });
    } catch (error) {
      addAlert(t("logoutAllSessionsError", { error }), AlertVariant.danger);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("signOutAllActiveSessionsQuestion")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="logout-all-confirm-button"
          key="set-to-now"
          variant="primary"
          onClick={() => {
            logoutAllSessions();
            handleModalToggle();
          }}
          form="revocation-modal-form"
        >
          {t("realm-settings:confirm")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <TextContent className="kc-logout-all-description-text">
        {t("logoutAllDescription")}
      </TextContent>
    </Modal>
  );
};
