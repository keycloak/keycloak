import React, { useContext } from "react";
import { useHistory } from "react-router-dom";
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
import { WhoAmIContext } from "../../context/whoami/WhoAmI";
import { FormAccess } from "../../components/form-access/FormAccess";

export const NewRealmForm = () => {
  const { t } = useTranslation("realm");
  const history = useHistory();
  const { refresh } = useContext(WhoAmIContext);
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState,
    errors,
  } = useForm<RealmRepresentation>({ mode: "onChange" });

  const handleFileChange = (value: string | File) => {
    const defaultRealm = { id: "", realm: "", enabled: true };

    let obj: { [name: string]: boolean | string } = defaultRealm;
    if (value) {
      try {
        obj = JSON.parse(value as string);
      } catch (error) {
        console.warn("Invalid json, ignoring value using default");
      }
    }
    Object.keys(obj).forEach((k) => {
      setValue(k, obj[k]);
    });
  };

  const save = async (realm: RealmRepresentation) => {
    try {
      await adminClient.realms.create(realm);
      addAlert(t("saveRealmSuccess"), AlertVariant.success);
      refresh();
      //force token update
      await adminClient.keycloak?.updateToken(Number.MAX_VALUE);
      history.push(`/${realm.realm}/`);
    } catch (error) {
      addAlert(
        t("saveRealmError", {
          error: error.response.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader titleKey="realm:createRealm" subKey="realm:realmExplain" />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="manage-realm"
        >
          <JsonFileUpload id="kc-realm-filename" onChange={handleFileChange} />
          <FormGroup
            label={t("realmName")}
            isRequired
            fieldId="kc-realm-name"
            validated={errors.realm ? "error" : "default"}
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              isRequired
              type="text"
              id="kc-realm-name"
              name="realm"
              validated={errors.realm ? "error" : "default"}
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
            <Button
              variant="primary"
              type="submit"
              isDisabled={!formState.isValid}
            >
              {t("common:create")}
            </Button>
            <Button variant="link" onClick={() => history.goBack()}>
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
