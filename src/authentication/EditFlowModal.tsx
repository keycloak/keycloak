import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { NameDescription } from "./form/NameDescription";

type EditFlowModalProps = {
  flow: AuthenticationFlowRepresentation;
  toggleDialog: () => void;
};

type EditForm = {
  alias: string;
  description: string;
};

export const EditFlowModal = ({ flow, toggleDialog }: EditFlowModalProps) => {
  const { t } = useTranslation("authentication");
  const form = useForm<EditForm>({
    shouldUnregister: false,
  });
  const { reset, handleSubmit } = form;
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    reset(flow);
  }, [flow, reset]);

  const save = async (values: EditForm) => {
    try {
      await adminClient.authenticationManagement.updateFlow(
        { flowId: flow.id! },
        { ...flow, ...values }
      );
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updateFlowError", error);
    }
    toggleDialog();
  };

  return (
    <Modal
      title={t("editFlow")}
      isOpen={true}
      onClose={toggleDialog}
      variant={ModalVariant.small}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          onClick={handleSubmit(save)}
          data-testid="confirm"
        >
          {t("edit")}
        </Button>,
        <Button
          data-testid="cancel"
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form isHorizontal>
          <NameDescription />
        </Form>
      </FormProvider>
    </Modal>
  );
};
