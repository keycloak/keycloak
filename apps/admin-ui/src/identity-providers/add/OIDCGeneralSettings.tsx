import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import { useParams } from "react-router-dom-v5-compat";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { RedirectUrl } from "../component/RedirectUrl";
import { TextField } from "../component/TextField";
import { DisplayOrder } from "../component/DisplayOrder";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import type { IdentityProviderParams } from "../routes/IdentityProvider";

export const OIDCGeneralSettings = ({ id }: { id: string }) => {
  const { t } = useTranslation("identity-providers");
  const { tab } = useParams<IdentityProviderParams>();

  const {
    register,
    formState: { errors },
  } = useFormContext();

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
          isReadOnly={tab === "settings"}
          isRequired
          type="text"
          id="alias"
          data-testid="alias"
          name="alias"
          validated={
            errors.alias ? ValidatedOptions.error : ValidatedOptions.default
          }
          ref={register({ required: true })}
        />
      </FormGroup>

      <TextField field="displayName" label="displayName" />
      <DisplayOrder />
    </>
  );
};
