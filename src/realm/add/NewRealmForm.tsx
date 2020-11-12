import React from "react";
import { useTranslation } from "react-i18next";
import {
  PageSection,
  FormGroup,
  Form,
  TextInput,
  Switch,
  ActionGroup,
  Button,
  AlertVariant,
} from "@patternfly/react-core";

import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useAlerts } from "../../components/alert/Alerts";
import { useForm, Controller } from "react-hook-form";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";

export const NewRealmForm = () => {
  const { t } = useTranslation("realm");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const { register, handleSubmit, setValue, control } = useForm<
    RealmRepresentation
  >();

  const handleFileChange = (value: string | File) => {
    const defaultRealm = { id: "", realm: "", enabled: true };

    const obj = value ? JSON.parse(value as string) : defaultRealm;
    Object.keys(obj).forEach((k) => {
      setValue(k, obj[k]);
    });
  };

  const save = async (realm: RealmRepresentation) => {
    try {
      await adminClient.realms.create(realm);
      addAlert(t("Realm created"), AlertVariant.success);
    } catch (error) {
      addAlert(
        `${t("Could not create realm:")} '${error}'`,
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader titleKey="realm:createRealm" subKey="realm:realmExplain" />
      <PageSection variant="light">
        <Form isHorizontal onSubmit={handleSubmit(save)}>
          <JsonFileUpload id="kc-realm-filename" onChange={handleFileChange} />
          <FormGroup label={t("realmName")} isRequired fieldId="kc-realm-name">
            <TextInput
              isRequired
              type="text"
              id="kc-realm-name"
              name="realm"
              ref={register({ required: true })}
            />
          </FormGroup>
          <FormGroup label={t("enabled")} fieldId="kc-realm-enabled-switch">
            <Controller
              name="enabled"
              defaultValue={true}
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-realm-enabled-switch"
                  name="enabled"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:create")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
