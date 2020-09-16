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

import { ClientRepresentation } from "../models/client-model";
import { ClientDescription } from "../ClientDescription";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useAlerts } from "../../components/alert/Alerts";

export const ImportForm = () => {
  const { t } = useTranslation("clients");
  const httpClient = useContext(HttpClientContext)!;

  const [add, Alerts] = useAlerts();
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
      add(t("clientImportSuccess"), AlertVariant.success);
    } catch (error) {
      add(`${t("clientImportError")} '${error}'`, AlertVariant.danger);
    }
  };
  return (
    <>
      <Alerts />
      <PageSection variant="light">
        <TextContent>
          <Text component="h1">{t("importClient")}</Text>
          {t("clientsExplain")}
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
          <FormGroup label={t("type")} fieldId="kc-type">
            <TextInput
              type="text"
              id="kc-type"
              name="protocol"
              value={client.protocol}
              isReadOnly
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" onClick={() => save()}>
              {t("common:save")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
