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

import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealms } from "../../context/RealmsContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

export default function NewRealmForm() {
  const { t } = useTranslation("realm");
  const navigate = useNavigate();
  const { refresh, whoAmI } = useWhoAmI();
  const { refresh: refreshRealms } = useRealms();
  const { adminClient } = useAdminClient();
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
      addError("realm:saveRealmError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey="realm:createRealm" subKey="realm:realmExplain" />
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
            label={t("realmName")}
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
                required: { value: true, message: t("common:required") },
                pattern: {
                  value: /^[a-zA-Z0-9-_]+$/,
                  message: t("invalidRealmName"),
                },
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
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                  aria-label={t("enabled")}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:create")}
            </Button>
            <Button variant="link" onClick={() => navigate(-1)}>
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
