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
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useForm } from "react-hook-form";

type GroupsCreateModalProps = {
  handleModalToggle: () => void;
  isCreateModalOpen: boolean;
  setIsCreateModalOpen: (isCreateModalOpen: boolean) => void;
  createGroupName: string;
  setCreateGroupName: (createGroupName: string) => void;
  refresh: () => void;
};

export const GroupsCreateModal = ({
  handleModalToggle,
  isCreateModalOpen,
  setIsCreateModalOpen,
  createGroupName,
  setCreateGroupName,
  refresh,
}: GroupsCreateModalProps) => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const form = useForm();
  const { register, errors } = form;

  const valueChange = (createGroupName: string) => {
    setCreateGroupName(createGroupName);
  };

  const submitForm = async () => {
    if (await form.trigger()) {
      try {
        await adminClient.groups.create({ name: createGroupName });
        refresh();
        setIsCreateModalOpen(false);
        setCreateGroupName("");
        addAlert(t("groupCreated"), AlertVariant.success);
      } catch (error) {
        addAlert(
          `${t("couldNotCreateGroup")} ': '${error}'`,
          AlertVariant.danger
        );
      }
    }
  };

  return (
    <>
      <Modal
        variant={ModalVariant.small}
        title={t("createAGroup")}
        isOpen={isCreateModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button key="confirm" variant="primary" onClick={() => submitForm()}>
            {t("create")}
          </Button>,
        ]}
      >
        <Form isHorizontal>
          <FormGroup
            name="create-modal-group"
            label={t("name")}
            fieldId="group-id"
            helperTextInvalid={t("common:required")}
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            isRequired
          >
            <TextInput
              ref={register({ required: true })}
              type="text"
              id="create-group-name"
              name="name"
              value={createGroupName}
              onChange={valueChange}
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
