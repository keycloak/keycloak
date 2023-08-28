import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { DisplayOrder } from "../component/DisplayOrder";
import { RedirectUrl } from "../component/RedirectUrl";
import { TextField } from "../component/TextField";
import type { IdentityProviderParams } from "../routes/IdentityProvider";
import { useState, ChangeEvent } from "react"

export const OIDCGeneralSettings = ({ id }: { id: string }) => {
  const { t } = useTranslation("identity-providers");
  const { tab } = useParams<IdentityProviderParams>();

  const {
    register,
    formState: { errors },
  } = useFormContext();

  const [aliasText, setAliasText] = useState(id);
  const onAliasChange = (event : ChangeEvent<HTMLInputElement>) => {
      setAliasText(event.target.value)
    };

  return (
    <>
      <RedirectUrl id={aliasText} />

      <FormGroup
        label={t("alias")}
        labelIcon={
          <HelpItem
            helpText={t("identity-providers-help:alias")}
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
          id="alias"
          data-testid="alias"
          validated={
            errors.alias ? ValidatedOptions.error : ValidatedOptions.default
          }
          {...register("alias", { required: true })}
          onChange={onAliasChange}
        />
      </FormGroup>

      <TextField field="displayName" label="displayName" />
      <DisplayOrder />
    </>
  );
};
