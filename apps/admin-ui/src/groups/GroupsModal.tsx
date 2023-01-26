import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";

type GroupsModalProps = {
  id?: string;
  rename?: string;
  handleModalToggle: () => void;
  refresh: (group?: GroupRepresentation) => void;
};

export const GroupsModal = ({
  id,
  rename,
  handleModalToggle,
  refresh,
}: GroupsModalProps) => {
  const { t } = useTranslation("groups");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: { name: rename },
  });

  const submitForm = async (group: GroupRepresentation) => {
    group.name = group.name?.trim();

    try {
      if (!id) {
        await adminClient.groups.create(group);
      } else if (rename) {
        await adminClient.groups.update({ id }, group);
      } else {
        await adminClient.groups.setOrCreateChild({ id }, group);
      }

      refresh(rename ? group : undefined);
      handleModalToggle();
      addAlert(
        t(rename ? "groupUpdated" : "groupCreated"),
        AlertVariant.success
      );
    } catch (error) {
      addError("groups:couldNotCreateGroup", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(rename ? "renameAGroup" : "createAGroup")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid={`${rename ? "rename" : "create"}Group`}
          key="confirm"
          variant="primary"
          type="submit"
          form="group-form"
        >
          {t(rename ? "rename" : "create")}
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
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form id="group-form" isHorizontal onSubmit={handleSubmit(submitForm)}>
        <FormGroup
          name="create-modal-group"
          label={t("common:name")}
          fieldId="create-group-name"
          helperTextInvalid={t("common:required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            data-testid="groupNameInput"
            autoFocus
            id="create-group-name"
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            {...register("name", { required: true })}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
