import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAccess } from "../../context/access/Access";
import { FormFields } from "../ClientDetails";
import type { ClientSettingsProps } from "../ClientSettings";
import { LoginSettings } from "./LoginSettings";

export const AccessSettings = ({
  client,
  save,
  reset,
}: ClientSettingsProps) => {
  const { t } = useTranslation();
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
            <HelpItem helpText={t("adminURLHelp")} fieldLabelId="adminURL" />
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
        <FixedButtonsGroup
          name="settings"
          save={save}
          reset={reset}
          isActive={!isManager}
        />
      )}
    </FormAccess>
  );
};
