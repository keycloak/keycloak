import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { RedirectUrl } from "../component/RedirectUrl";
import { TextField } from "../component/TextField";
import { DisplayOrder } from "../component/DisplayOrder";
import { FormattedLink } from "../../components/external-link/FormattedLink";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useRealm } from "../../context/realm-context/RealmContext";
import environment from "../../environment";

import "./saml-general-settings.css";

type SamlGeneralSettingsProps = {
  id: string;
  isAliasReadonly?: boolean;
};

export const SamlGeneralSettings = ({
  id,
  isAliasReadonly = false,
}: SamlGeneralSettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const { realm } = useRealm();

  const {
    register,
    watch,
    formState: { errors },
  } = useFormContext();

  const alias = watch("alias");

  return (
    <>
      <RedirectUrl id={id} />

      <FormGroup
        label={t("alias")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:alias"
            fieldLabelId="identity-providers:alias"
          />
        }
        fieldId="alias"
        isRequired
        validated={
          errors.alias ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          isRequired
          type="text"
          id="alias"
          data-testid="alias"
          name="alias"
          isReadOnly={isAliasReadonly}
          validated={
            errors.alias ? ValidatedOptions.error : ValidatedOptions.default
          }
          ref={register({ required: true })}
        />
      </FormGroup>

      <TextField field="displayName" label="displayName" />
      <DisplayOrder />
      <FormGroup
        label={t("endpoints")}
        fieldId="endpoints"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:alias"
            fieldLabelId="identity-providers:alias"
          />
        }
        className="keycloak__identity-providers__saml_link"
      >
        <FormattedLink
          title={t("samlEndpointsLabel")}
          href={`${environment.authUrl}/realms/${realm}/broker/${alias}/endpoint/descriptor`}
          isInline
        />
      </FormGroup>
    </>
  );
};
