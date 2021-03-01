import React from "react";
import {
  AlertVariant,
  Button,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

type GroupsCreateModalProps = {
  id?: string;
  handleModalToggle: () => void;
  refresh: () => void;
};

export const GroupsCreateModal = ({
  id,
  handleModalToggle,
  refresh,
}: GroupsCreateModalProps) => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { register, errors, handleSubmit } = useForm();

  const submitForm = async (group: GroupRepresentation) => {
    try {
      if (!id) {
        await adminClient.groups.create({ name: group.name });
      } else {
        await adminClient.groups.setOrCreateChild({ id }, { name: group.name });
      }

      refresh();
      handleModalToggle();
      addAlert(t("groupCreated"), AlertVariant.success);
    } catch (error) {
      addAlert(t("couldNotCreateGroup", { error }), AlertVariant.danger);
    }
  };

  return (
    <>
      <Modal
        variant={ModalVariant.small}
        title={t("createAGroup")}
        isOpen={true}
        onClose={handleModalToggle}
        actions={[
          <Button
            data-testid="createGroup"
            key="confirm"
            variant="primary"
            type="submit"
            form="group-form"
          >
            {t("create")}
          </Button>,
        ]}
      >
        <Form id="group-form" isHorizontal onSubmit={handleSubmit(submitForm)}>
          <FormGroup
            name="create-modal-group"
            label={t("common:name")}
            fieldId="group-id"
            helperTextInvalid={t("common:required")}
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            isRequired
          >
            <TextInput
              data-testid="groupNameInput"
              ref={register({ required: true })}
              autoFocus
              type="text"
              id="create-group-name"
              name="name"
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
            />
          </FormGroup>
        </Form>
      </Modal>
    </>
  );
};
