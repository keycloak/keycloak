import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type FormFields = {
  node: string;
};

type AddHostDialogProps = {
  clientId: string;
  isOpen: boolean;
  onAdded: (host: string) => void;
  onClose: () => void;
};

export const AddHostDialog = ({
  clientId: id,
  isOpen,
  onAdded,
  onClose,
}: AddHostDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<FormFields>();
  const {
    handleSubmit,
    formState: { isDirty, isValid },
  } = form;
  const { addAlert, addError } = useAlerts();

  async function onSubmit({ node }: FormFields) {
    try {
      await adminClient.clients.addClusterNode({
        id,
        node,
      });
      onAdded(node);
      addAlert(t("addedNodeSuccess"), AlertVariant.success);
    } catch (error) {
      addError("addedNodeFail", error);
    }

    onClose();
  }

  return (
    <Modal
      title={t("addNode")}
      isOpen={isOpen}
      onClose={onClose}
      variant="small"
      actions={[
        <Button
          key="confirm"
          id="add-node-confirm"
          type="submit"
          form="add-host-form"
          isDisabled={!isDirty || !isValid}
        >
          {t("save")}
        </Button>,
        <Button
          key="cancel"
          id="add-node-cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="add-host-form" onSubmit={handleSubmit(onSubmit)} isHorizontal>
          <TextControl
            name="node"
            label={t("nodeHost")}
            rules={{
              required: t("required"),
            }}
          />
        </Form>
      </FormProvider>
    </Modal>
  );
};
