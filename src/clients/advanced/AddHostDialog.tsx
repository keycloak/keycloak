import React from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  TextInput,
} from "@patternfly/react-core";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";

type Host = {
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
  const { t } = useTranslation("clients");
  const { register, getValues } = useForm<Host>();
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  return (
    <Modal
      title={t("addNode")}
      isOpen={isOpen}
      onClose={onClose}
      variant="small"
      actions={[
        <Button
          id="add-node-confirm"
          key="confirm"
          onClick={async () => {
            try {
              const node = getValues("node");
              await adminClient.clients.addClusterNode({
                id,
                node,
              });
              onAdded(node);
              addAlert(t("addedNodeSuccess"), AlertVariant.success);
            } catch (error) {
              addAlert(
                t("addedNodeFail", {
                  error: error.response?.data?.error || error,
                }),
                AlertVariant.danger
              );
            }

            onClose();
          }}
        >
          {t("common:save")}
        </Button>,
        <Button
          id="add-node-cancel"
          key="cancel"
          variant={ButtonVariant.secondary}
          onClick={() => onClose()}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form isHorizontal>
        <FormGroup label={t("nodeHost")} fieldId="nodeHost">
          <TextInput id="nodeHost" ref={register} name="node" />
        </FormGroup>
      </Form>
    </Modal>
  );
};
