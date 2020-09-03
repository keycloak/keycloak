import React, { useState, FormEvent, useContext } from "react";
import {
  PageSection,
  Text,
  TextContent,
  Divider,
  Form,
  FormGroup,
  FileUpload,
  TextInput,
  ActionGroup,
  Button,
  AlertVariant,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { ClientRepresentation } from "../../model/client-model";
import { ClientDescription } from "./ClientDescription";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { useAlerts } from "../../components/alert/Alerts";
import { AlertPanel } from "../../components/alert/AlertPanel";

type FileUpload = {
  value: string | File;
  filename: string;
  isLoading: boolean;
  modal: boolean;
};

export const ImportForm = () => {
  const { t } = useTranslation();
  const httpClient = useContext(HttpClientContext)!;

  const [add, alerts, hide] = useAlerts();
  const defaultUpload = {
    value: "",
    filename: "",
    isLoading: false,
    modal: false,
  };
  const [fileUpload, setFileUpload] = useState<FileUpload>(defaultUpload);
  const defaultClient = {
    protocol: "",
    clientId: "",
    name: "",
    description: "",
  };
  const [client, setClient] = useState<ClientRepresentation>(defaultClient);

  const handleFileChange = (value: string | File, filename: string) => {
    if (value === "" && client.protocol !== "") {
      // clear clicked
      setFileUpload({ ...fileUpload, modal: true });
    } else {
      setFileUpload({
        ...fileUpload,
        value,
        filename,
      });
      setClient({
        ...client,
        ...(value ? JSON.parse(value as string) : defaultClient),
      });
    }
  };
  const handleDescriptionChange = (
    value: string,
    event: FormEvent<HTMLInputElement>
  ) => {
    const name = (event.target as HTMLInputElement).name;
    setClient({ ...client, [name]: value });
  };
  const removeDialog = () => setFileUpload({ ...fileUpload, modal: false });

  const save = async () => {
    try {
      await httpClient.doPost("/admin/realms/master/clients", client);
      add(t("Client imported"), AlertVariant.success);
    } catch (error) {
      add(`${t("Could not import client:")} '${error}'`, AlertVariant.danger);
    }
  };
  return (
    <>
      <AlertPanel alerts={alerts} onCloseAlert={hide} />
      <PageSection variant="light">
        <TextContent>
          <Text component="h1">{t("Import client")}</Text>
          {t(
            "Clients are applications and services that can request authentication of a user"
          )}
        </TextContent>
      </PageSection>
      <Divider />
      <PageSection variant="light">
        {fileUpload.modal && (
          <Modal
            variant={ModalVariant.small}
            title={t("Clear this file")}
            isOpen
            onClose={removeDialog}
            actions={[
              <Button
                key="confirm"
                variant="primary"
                onClick={() => {
                  setClient(defaultClient);
                  setFileUpload(defaultUpload);
                }}
              >
                {t("Clear")}
              </Button>,
              <Button key="cancel" variant="link" onClick={removeDialog}>
                {t("Cancel")}
              </Button>,
            ]}
          >
            {t("confirmImportClear")}
          </Modal>
        )}
        <Form isHorizontal>
          <FormGroup
            label={t("Resource file")}
            fieldId="realm-file"
            helperText="Upload a JSON file"
          >
            <FileUpload
              id="realm-file"
              type="text"
              value={fileUpload.value}
              filename={fileUpload.filename}
              onChange={handleFileChange}
              allowEditingUploadedText
              onReadStarted={() =>
                setFileUpload({ ...fileUpload, isLoading: true })
              }
              onReadFinished={() =>
                setFileUpload({ ...fileUpload, isLoading: false })
              }
              isLoading={fileUpload.isLoading}
              dropzoneProps={{
                accept: ".json",
              }}
            />
          </FormGroup>
          <ClientDescription
            onChange={handleDescriptionChange}
            client={client}
          />
          <FormGroup label={t("Type")} fieldId="kc-type">
            <TextInput
              type="text"
              id="kc-type"
              name="protocol"
              value={client.protocol}
              isReadOnly
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" onClick={save}>
              {t("Save")}
            </Button>
            <Button variant="link">{t("Cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
