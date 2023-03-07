import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAccess } from "../../context/access/Access";
import { SaveReset } from "../advanced/SaveReset";
import { FormFields } from "../ClientDetails";
import type { ClientSettingsProps } from "../ClientSettings";
import { LoginSettings } from "./LoginSettings";

export const AccessSettings = ({
  client,
  save,
  reset,
}: ClientSettingsProps) => {
  const { t } = useTranslation("clients");
  const { register, watch } = useFormContext<FormFields>();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || client.access?.configure;

  const protocol = watch("protocol");

  return (
    <FormAccess
      isHorizontal
      fineGrainedAccess={client.access?.configure}
      role="manage-clients"
    >
      {!client.bearerOnly && <LoginSettings protocol={protocol} />}
      {protocol !== "saml" && (
        <FormGroup
          label={t("adminURL")}
          fieldId="kc-admin-url"
          labelIcon={
            <HelpItem
              helpText={t("clients-help:adminURL")}
              fieldLabelId="clients:adminURL"
            />
          }
        >
          <KeycloakTextInput
            id="kc-admin-url"
            type="url"
            {...register("adminUrl")}
          />
        </FormGroup>
      )}
      {client.bearerOnly && (
        <SaveReset
          className="keycloak__form_actions"
          name="settings"
          save={save}
          reset={reset}
          isActive={!isManager}
        />
      )}
    </FormAccess>
  );
};
