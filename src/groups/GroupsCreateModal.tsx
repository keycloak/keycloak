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
import { HttpClientContext } from "../http-service/HttpClientContext";
import { RealmContext } from "../components/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { useForm } from "react-hook-form";

type GroupsCreateModalProps = {
  handleModalToggle: () => void;
  isCreateModalOpen: boolean;
  setIsCreateModalOpen: (isCreateModalOpen: boolean) => void;
  createGroupName: string;
  setCreateGroupName: (createGroupName: string) => void;
};

export const GroupsCreateModal = ({
  handleModalToggle,
  isCreateModalOpen,
  setIsCreateModalOpen,
  createGroupName,
  setCreateGroupName,
}: GroupsCreateModalProps) => {
  const { t } = useTranslation("groups");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const [add, Alerts] = useAlerts();
  const form = useForm();
  const { register, errors } = form;

  const valueChange = (createGroupName: string) => {
    setCreateGroupName(createGroupName);
  };

  const submitForm = async () => {
    try {
      await httpClient.doPost(`/admin/realms/${realm}/groups`, {
        name: createGroupName,
      });
      setIsCreateModalOpen(false);
      setCreateGroupName("");
      add(t("groupCreated"), AlertVariant.success);
    } catch (error) {
      add(`${t("couldNotCreateGroup")} ': '${error}'`, AlertVariant.danger);
    }
  };

  return (
    <React.Fragment>
      <Alerts />
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
    </React.Fragment>
  );
};
