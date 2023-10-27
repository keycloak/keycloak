import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealms } from "../../context/RealmsContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

export default function NewRealmForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { refresh, whoAmI } = useWhoAmI();
  const { refresh: refreshRealms } = useRealms();
  const { addAlert, addError } = useAlerts();
  const [realm, setRealm] = useState<RealmRepresentation>();

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<RealmRepresentation>({ mode: "onChange" });

  const handleFileChange = (obj?: object) => {
    const defaultRealm = { id: "", realm: "", enabled: true };
    convertToFormValues(obj || defaultRealm, setValue);
    setRealm(obj || defaultRealm);
  };

  const save = async (fields: RealmRepresentation) => {
    try {
      await adminClient.realms.create({
        ...realm,
        ...convertFormValuesToObject(fields),
      });
      addAlert(t("saveRealmSuccess"));

      refresh();
      await refreshRealms();
      navigate(toDashboard({ realm: fields.realm }));
    } catch (error) {
      addError("saveRealmError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey="createRealm" subKey="realmExplain" />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="view-realm"
          isReadOnly={!whoAmI.canCreateRealm()}
        >
          <JsonFileUpload
            id="kc-realm-filename"
            allowEditingUploadedText
            onChange={handleFileChange}
          />
          <FormGroup
            label={t("realmNameField")}
            isRequired
            fieldId="kc-realm-name"
            validated={errors.realm ? "error" : "default"}
            helperTextInvalid={errors.realm?.message}
          >
            <KeycloakTextInput
              isRequired
              id="kc-realm-name"
              validated={errors.realm ? "error" : "default"}
              {...register("realm", {
                required: { value: true, message: t("required") },
              })}
            />
          </FormGroup>
          <FormGroup label={t("enabled")} fieldId="kc-realm-enabled-switch">
            <Controller
              name="enabled"
              defaultValue={true}
              control={control}
              render={({ field }) => (
                <Switch
                  id="kc-realm-enabled-switch"
                  name="enabled"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                  aria-label={t("enabled")}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("create")}
            </Button>
            <Button variant="link" onClick={() => navigate(-1)}>
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
