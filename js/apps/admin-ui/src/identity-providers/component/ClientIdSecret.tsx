import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, PasswordControl } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const ClientIdSecret = ({
  secretRequired = true,
  create = true,
}: {
  secretRequired?: boolean;
  create?: boolean;
}) => {
  const { t } = useTranslation();

  const {
    register,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();

  return (
    <>
      <FormGroup
        label={t("clientId")}
        labelIcon={
          <HelpItem helpText={t("clientIdHelp")} fieldLabelId="clientId" />
        }
        fieldId="kc-client-id"
        isRequired
        validated={
          errors.config?.clientId
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("required")}
      >
        <KeycloakTextInput
          isRequired
          id="kc-client-id"
          data-testid="clientId"
          {...register("config.clientId", { required: true })}
        />
      </FormGroup>
      <PasswordControl
        name="config.clientSecret"
        label={t("clientSecret")}
        labelIcon={t("clientSecretHelp")}
        hasReveal={create}
        rules={{ required: { value: secretRequired, message: t("required") } }}
      />
    </>
  );
};
