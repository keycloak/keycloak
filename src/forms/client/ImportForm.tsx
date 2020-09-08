import React, { useState, FormEvent, useContext } from "react";
import {
  PageSection,
  Text,
  TextContent,
  Divider,
  Form,
  FormGroup,
  TextInput,
  ActionGroup,
  Button,
  AlertVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { ClientRepresentation } from "../../model/client-model";
import { ClientDescription } from "./ClientDescription";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useAlerts } from "../../components/alert/Alerts";
import { AlertPanel } from "../../components/alert/AlertPanel";

export const ImportForm = () => {
  const { t } = useTranslation();
  const httpClient = useContext(HttpClientContext)!;

  const [add, alerts, hide] = useAlerts();
  const defaultClient = {
    protocol: "",
    clientId: "",
    name: "",
    description: "",
  };
  const [client, setClient] = useState<ClientRepresentation>(defaultClient);

  const handleFileChange = (value: string | File) => {
    setClient({
      ...client,
      ...(value ? JSON.parse(value as string) : defaultClient),
    });
  };
  const handleDescriptionChange = (
    value: string,
    event: FormEvent<HTMLInputElement>
  ) => {
    const name = (event.target as HTMLInputElement).name;
    setClient({ ...client, [name]: value });
  };

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
        <Form isHorizontal>
          <JsonFileUpload id="realm-file" onChange={handleFileChange} />
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
