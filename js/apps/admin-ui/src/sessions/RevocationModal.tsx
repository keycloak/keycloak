import type GlobalRequestResult from "@keycloak/keycloak-admin-client/lib/defs/globalRequestResult";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";

type RevocationModalProps = {
  handleModalToggle: () => void;
  save: () => void;
};

export const RevocationModal = ({
  handleModalToggle,
  save,
}: RevocationModalProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const { realm: realmName, realmRepresentation: realm, refresh } = useRealm();
  const { register, handleSubmit } = useForm();

  const parseResult = (result: GlobalRequestResult, prefixKey: string) => {
    const successCount = result.successRequests?.length || 0;
    const failedCount = result.failedRequests?.length || 0;

    if (successCount === 0 && failedCount === 0) {
      addAlert(t("noAdminUrlSet"), AlertVariant.warning);
    } else if (failedCount > 0) {
      addAlert(
        t(prefixKey + "Success", {
          successNodes: result.successRequests,
        }),
        AlertVariant.success,
      );
      addAlert(
        t(prefixKey + "Fail", {
          failedNodes: result.failedRequests,
        }),
        AlertVariant.danger,
      );
    } else {
      addAlert(
        t(prefixKey + "Success", {
          successNodes: result.successRequests,
        }),
        AlertVariant.success,
      );
    }
  };

  const setToNow = async () => {
    try {
      await adminClient.realms.update(
        { realm: realmName },
        {
          realm: realmName,
          notBefore: Date.now() / 1000,
        },
      );

      addAlert(t("notBeforeSuccess"), AlertVariant.success);
    } catch (error) {
      addError("setToNowError", error);
    }
  };

  const clearNotBefore = async () => {
    try {
      await adminClient.realms.update(
        { realm: realmName },
        {
          realm: realmName,
          notBefore: 0,
        },
      );
      addAlert(t("notBeforeClearedSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("notBeforeError", error);
    }
  };

  const push = async () => {
    const result = await adminClient.realms.pushRevocation({
      realm: realmName,
    });
    parseResult(result, "notBeforePush");

    refresh();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("revocation")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="set-to-now-button"
          key="set-to-now"
          variant="tertiary"
          onClick={async () => {
            await setToNow();
            handleModalToggle();
          }}
          form="revocation-modal-form"
        >
          {t("setToNow")}
        </Button>,
        <Button
          data-testid="clear-not-before-button"
          key="clear"
          variant="tertiary"
          onClick={async () => {
            await clearNotBefore();
            handleModalToggle();
          }}
          form="revocation-modal-form"
        >
          {t("clear")}
        </Button>,
        <Button
          data-testid="modal-test-connection-button"
          key="push"
          variant="secondary"
          onClick={async () => {
            await push();
            handleModalToggle();
          }}
          form="revocation-modal-form"
        >
          {t("push")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <TextContent className="kc-revocation-description-text">
        {t("revocationDescription")}
      </TextContent>
      <Form
        id="revocation-modal-form"
        isHorizontal
        onSubmit={handleSubmit(save)}
      >
        <FormGroup
          className="kc-revocation-modal-form-group"
          label={t("notBefore")}
          name="notBefore"
          fieldId="not-before"
        >
          <TextInput
            data-testid="not-before-input"
            autoFocus
            readOnly
            value={
              realm?.notBefore === 0
                ? (t("none") as string)
                : new Date(realm?.notBefore! * 1000).toString()
            }
            type="text"
            id="not-before"
            {...register("notBefore")}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
