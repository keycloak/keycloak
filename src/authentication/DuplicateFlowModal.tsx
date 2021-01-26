import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

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
  const { register, errors, setValue, trigger, getValues } = useForm({
    shouldUnregister: false,
  });
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

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
      addAlert(t("copyFlowError", { error }), AlertVariant.danger);
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
        <Button id="modal-confirm" key="confirm" onClick={save}>
          {t("common:save")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.secondary}
          onClick={() => {
            toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form isHorizontal>
        <FormGroup
          label={t("common:name")}
          fieldId="kc-name"
          helperTextInvalid={t("common:required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <TextInput
            type="text"
            id="kc-name"
            name="name"
            ref={register({ required: true })}
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup label={t("common:description")} fieldId="kc-description">
          <TextInput
            type="text"
            id="kc-description"
            name="description"
            ref={register()}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
