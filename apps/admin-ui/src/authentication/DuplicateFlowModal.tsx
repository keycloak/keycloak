import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom-v5-compat";
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
import { toFlow } from "./routes/Flow";
import { useRealm } from "../context/realm-context/RealmContext";

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
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();

  useEffect(() => {
    setValue("description", description);
    setValue("alias", t("copyOf", { name }));
  }, [name, description, setValue]);

  const save = async () => {
    if (!(await trigger())) return;
    const form = getValues();
    try {
      await adminClient.authenticationManagement.copyFlow({
        flow: name,
        newName: form.alias,
      });
      const newFlow = (
        await adminClient.authenticationManagement.getFlows()
      ).find((flow) => flow.alias === form.alias)!;

      if (form.description !== description) {
        newFlow.description = form.description;
        await adminClient.authenticationManagement.updateFlow(
          { flowId: newFlow.id! },
          newFlow
        );
      }
      addAlert(t("copyFlowSuccess"), AlertVariant.success);
      navigate(
        toFlow({
          realm,
          id: newFlow.id!,
          usedBy: "notInUse",
          builtIn: newFlow.builtIn ? "builtIn" : undefined,
        })
      );
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
