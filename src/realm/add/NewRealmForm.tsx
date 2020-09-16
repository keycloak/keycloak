import React, { useState, FormEvent, useContext } from "react";
import { useTranslation } from "react-i18next";
import {
  Text,
  PageSection,
  TextContent,
  FormGroup,
  Form,
  TextInput,
  Switch,
  ActionGroup,
  Button,
  Divider,
  AlertVariant,
} from "@patternfly/react-core";

import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { RealmRepresentation } from "../models/Realm";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { useAlerts } from "../../components/alert/Alerts";

export const NewRealmForm = () => {
  const { t } = useTranslation("realm");
  const httpClient = useContext(HttpClientContext)!;
  const [add, Alerts] = useAlerts();

  const defaultRealm = { id: "", realm: "", enabled: true };
  const [realm, setRealm] = useState<RealmRepresentation>(defaultRealm);

  const handleFileChange = (value: string | File) => {
    setRealm({
      ...realm,
      ...(value ? JSON.parse(value as string) : defaultRealm),
    });
  };
  const handleChange = (
    value: string | boolean,
    event: FormEvent<HTMLInputElement>
  ) => {
    const name = (event.target as HTMLInputElement).name;
    setRealm({ ...realm, [name]: value });
  };

  const save = async () => {
    try {
      await httpClient.doPost("/admin/realms", realm);
      add(t("Realm created"), AlertVariant.success);
    } catch (error) {
      add(`${t("Could not create realm:")} '${error}'`, AlertVariant.danger);
    }
  };

  return (
    <>
      <Alerts />
      <PageSection variant="light">
        <TextContent>
          <Text component="h1">Create Realm</Text>
        </TextContent>
      </PageSection>
      <Divider />
      <PageSection variant="light">
        <Form isHorizontal>
          <JsonFileUpload id="kc-realm-filename" onChange={handleFileChange} />
          <FormGroup label={t("realmName")} isRequired fieldId="kc-realm-name">
            <TextInput
              isRequired
              type="text"
              id="kc-realm-name"
              name="realm"
              value={realm.realm}
              onChange={handleChange}
            />
          </FormGroup>
          <FormGroup label={t("enabled")} fieldId="kc-realm-enabled-switch">
            <Switch
              id="kc-realm-enabled-switch"
              name="enabled"
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.enabled}
              onChange={handleChange}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" onClick={() => save()}>
              {t("create")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
