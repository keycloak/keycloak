import React, { useEffect } from "react";
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

import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { NameDescription } from "./form/NameDescription";

type DuplicateFlowModalProps = {
  name: string;
  description: string;
  toggleDialog: () => void;
  onComplete: () => void;
};

export const DuplicateFlowModal = ({
  name,
  description,
  toggleDialog,
  onComplete,
}: DuplicateFlowModalProps) => {
  const { t } = useTranslation("authentication");
  const form = useForm({
    shouldUnregister: false,
  });
  const { setValue, trigger, getValues } = form;
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    setValue("description", description);
    setValue("name", t("copyOf", { name }));
  }, [name, description, setValue]);

  const save = async () => {
    await trigger();
    const form = getValues();
    try {
      await adminClient.authenticationManagement.copyFlow({
        flow: name,
        newName: form.name,
      });
      if (form.description !== description) {
        const newFlow = (
          await adminClient.authenticationManagement.getFlows()
        ).find((flow) => flow.alias === form.name)!;

        newFlow.description = form.description;
        await adminClient.authenticationManagement.updateFlow(
          { flowId: newFlow.id! },
          newFlow
        );
      }
      addAlert(t("copyFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:copyFlowError", error);
    }
    onComplete();
  };

  return (
    <Modal
      title={t("duplicateFlow")}
      isOpen={true}
      onClose={toggleDialog}
      variant={ModalVariant.small}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          onClick={save}
          data-testid="confirm"
        >
          {t("duplicate")}
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
