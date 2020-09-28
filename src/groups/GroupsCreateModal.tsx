import React, {useState, useContext} from "react";
import {
  AlertVariant,
  Button,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HttpClientContext } from "../http-service/HttpClientContext";
import { RealmContext } from "../components/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";

export const GroupsCreateModal = ({isCreateModalOpen, handleModalToggle}) => {
  
  const { t } = useTranslation("groups");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const [ nameValue, setNameValue ] = useState("");
  const [add, Alerts] = useAlerts();

  const valueChange = (nameValue: string) => {
    setNameValue(nameValue)
  }

  const submitForm = async () => {
    try {
      await httpClient.doPost(`/admin/realms/${realm}/groups`, nameValue);
      add("Client created", AlertVariant.success);
    } catch (error) {
      add(`Could not create client: '${error}'`, AlertVariant.danger);
    }
  }

  return (
    <Modal
      variant={ModalVariant.small}
      title="Create a group"
      isOpen={isCreateModalOpen}
      onClose={handleModalToggle}
      actions={[
        <Button key="confirm" variant="primary" onClick={() => submitForm()}>
          Create
        </Button>
      ]}
    >
      <Form isHorizontal>
        <FormGroup label={t("name")} fieldId="kc-root-url">
          <TextInput
            type="text"
            id="create-group-name"
            name="Name"
            value={nameValue}
            onChange={valueChange}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
