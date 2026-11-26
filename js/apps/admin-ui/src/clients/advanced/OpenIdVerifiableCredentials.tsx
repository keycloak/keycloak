import { Button, ActionGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormAccess } from "../../components/form/FormAccess";
import { IdentityProviderSelect } from "../../components/identity-provider/IdentityProviderSelect";
import { convertAttributeNameToForm } from "../../util";
import { FormFields, SaveOptions } from "../ClientDetails";
import ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { IdentityProviderType } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";

type OpenIdVerifiableCredentialsProps = {
  client: ClientRepresentation;
  save: (options?: SaveOptions) => void;
  reset: () => void;
};

export const OpenIdVerifiableCredentials = ({
  save,
  reset,
}: OpenIdVerifiableCredentialsProps) => {
  const { t } = useTranslation();
  const { watch } = useFormContext();

  const oid4vciEnabled = watch(
    convertAttributeNameToForm<FormFields>("attributes.oid4vci.enabled"),
    false,
  );

  return (
    <FormAccess role="manage-clients" isHorizontal>
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.oid4vci.enabled",
        )}
        label={t("oid4vciEnabled")}
        labelIcon={t("oid4vciEnabledHelp")}
        stringify
      />

      {oid4vciEnabled === "true" && (
        <IdentityProviderSelect
          name={convertAttributeNameToForm<FormFields>(
            "attributes.oid4vci.attester_trust_idps",
          )}
          label={t("oid4vciAttesterTrustIdps")}
          helpText={t("oid4vciAttesterTrustIdpsHelp")}
          convertToName={convertAttributeNameToForm}
          identityProviderType={IdentityProviderType.TRUST_MATERIAL}
          realmOnly
          stringify
          stringifySeparator={","}
        />
      )}

      <ActionGroup>
        <Button
          variant="secondary"
          id="oid4vciSave"
          data-testid="oid4vciSave"
          onClick={() => save()}
        >
          {t("save")}
        </Button>
        <Button
          id="oid4vciRevert"
          data-testid="oid4vciRevert"
          variant="link"
          onClick={reset}
        >
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
