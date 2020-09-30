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

export const GroupsCreateModal = ({isCreateModalOpen, handleModalToggle, setIsCreateModalOpen, createGroupName, setCreateGroupName}) => {
  
  const { t } = useTranslation("groups");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const [ nameValue, setNameValue ] = useState("");
  const [add, Alerts] = useAlerts();

  const valueChange = (createGroupName: string) => {
    setCreateGroupName(createGroupName)
  }

  const submitForm = async () => {
    try {
      await httpClient.doPost(`/admin/realms/${realm}/groups`, {name: createGroupName});
      setIsCreateModalOpen(false);
      setCreateGroupName("");
      add("Client created", AlertVariant.success);
      // window.location.reload(false);
    } catch (error) {
      add(`Could not create client: '${error}'`, AlertVariant.danger);
    }
  }

  return (
    <React.Fragment>
      <Alerts />
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
              value={createGroupName}
              onChange={valueChange}
            />
          </FormGroup>
        </Form>
      </Modal>
    </React.Fragment>
  );
};
