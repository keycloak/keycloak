import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
} from "@patternfly/react-core";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

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
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

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
              addError("clients:addedNodeFail", error);
            }

            onClose();
          }}
        >
          {t("common:save")}
        </Button>,
        <Button
          id="add-node-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => onClose()}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form isHorizontal>
        <FormGroup label={t("nodeHost")} fieldId="nodeHost">
          <KeycloakTextInput id="nodeHost" ref={register} name="node" />
        </FormGroup>
      </Form>
    </Modal>
  );
};
