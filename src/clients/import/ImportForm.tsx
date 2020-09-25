import React, { useContext } from "react";
import {
  PageSection,
  Form,
  FormGroup,
  TextInput,
  ActionGroup,
  Button,
  AlertVariant,
} from "@patternfly/react-core";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { ClientRepresentation } from "../models/client-model";
import { ClientDescription } from "../ClientDescription";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useAlerts } from "../../components/alert/Alerts";
import { RealmContext } from "../../components/realm-context/RealmContext";
import { ViewHeader } from "../../components/view-header/ViewHeader";

export const ImportForm = () => {
  const { t } = useTranslation("clients");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const form = useForm<ClientRepresentation>();
  const { register, handleSubmit, setValue } = form;

  const [add, Alerts] = useAlerts();

  const handleFileChange = (value: string | File) => {
    const defaultClient = {
      protocol: "",
      clientId: "",
      name: "",
      description: "",
    };

    const obj = value ? JSON.parse(value as string) : defaultClient;
    Object.keys(obj).forEach((k) => {
      setValue(k, obj[k]);
    });
  };

  const save = async (client: ClientRepresentation) => {
    try {
      await httpClient.doPost(`/admin/realms/${realm}/clients`, client);
      add(t("clientImportSuccess"), AlertVariant.success);
    } catch (error) {
      add(`${t("clientImportError")} '${error}'`, AlertVariant.danger);
    }
  };
  return (
    <>
      <Alerts />
      <ViewHeader
        titleKey="clients:importClient"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light">
        <Form isHorizontal onSubmit={handleSubmit(save)}>
          <JsonFileUpload id="realm-file" onChange={handleFileChange} />
          <ClientDescription form={form} />
          <FormGroup label={t("type")} fieldId="kc-type">
            <TextInput
              type="text"
              id="kc-type"
              name="protocol"
              isReadOnly
              ref={register()}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:save")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
