import React, { useContext } from "react";
import {
  AlertVariant,
  Button,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { RealmContext } from "../context/realm-context/RealmContext";
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
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const { addAlert } = useAlerts();
  const form = useForm();
  const { register, errors } = form;

  const valueChange = (createGroupName: string) => {
    setCreateGroupName(createGroupName);
  };

  const submitForm = async () => {
    if (await form.trigger()) {
      try {
        await httpClient.doPost(`/admin/realms/${realm}/groups`, {
          name: createGroupName,
        });
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
            validated={errors.name ? "error" : "default"}
            isRequired
          >
            <TextInput
              ref={register({ required: true })}
              type="text"
              id="create-group-name"
              name="name"
              value={createGroupName}
              onChange={valueChange}
            />
          </FormGroup>
        </Form>
      </Modal>
    </>
  );
};
