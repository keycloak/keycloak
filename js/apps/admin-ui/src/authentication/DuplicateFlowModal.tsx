import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { NameDescription } from "./form/NameDescription";
import { toFlow } from "./routes/Flow";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<AuthenticationFlowRepresentation>({
    mode: "onChange",
    defaultValues: {
      alias: t("copyOf", { name }),
      description: description,
    },
  });
  const { getValues, handleSubmit } = form;
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const onSubmit = async () => {
    const form = getValues();
    try {
      await adminClient.authenticationManagement.copyFlow({
        flow: name,
        newName: form.alias!,
      });
      const newFlow = (
        await adminClient.authenticationManagement.getFlows()
      ).find((flow) => flow.alias === form.alias)!;

      if (form.description !== description) {
        newFlow.description = form.description;
        await adminClient.authenticationManagement.updateFlow(
          { flowId: newFlow.id! },
          newFlow,
        );
      }
      addAlert(t("copyFlowSuccess"), AlertVariant.success);
      navigate(
        toFlow({
          realm,
          id: newFlow.id!,
          usedBy: "notInUse",
          builtIn: newFlow.builtIn ? "builtIn" : undefined,
        }),
      );
    } catch (error) {
      addError("copyFlowError", error);
    }
    onComplete();
  };

  return (
    <Modal
      title={t("duplicateFlow")}
      onClose={toggleDialog}
      variant={ModalVariant.small}
      actions={[
        <Button
          key="confirm"
          data-testid="confirm"
          type="submit"
          form="duplicate-flow-form"
        >
          {t("duplicate")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={toggleDialog}
        >
          {t("cancel")}
        </Button>,
      ]}
      isOpen
    >
      <FormProvider {...form}>
        <Form
          id="duplicate-flow-form"
          onSubmit={handleSubmit(onSubmit)}
          isHorizontal
        >
          <NameDescription />
        </Form>
      </FormProvider>
    </Modal>
  );
};
