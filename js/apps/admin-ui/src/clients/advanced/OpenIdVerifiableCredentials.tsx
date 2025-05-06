import {
  FormGroup,
  Text,
  Button,
  ActionGroup,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { useFormContext } from "react-hook-form";
import { convertAttributeNameToForm } from "../../util";
import { FormFields, SaveOptions } from "../ClientDetails";
import ClientRepresentation from "libs/keycloak-admin-client/lib/defs/clientRepresentation";
import { DefaultSwitchControl } from "../../components/SwitchControl";

type OpenIdVerifiableCredentialsProps = {
  client: ClientRepresentation;
  save: (options?: SaveOptions) => void;
  reset: () => void;
};

export const OpenIdVerifiableCredentials = ({
  client,
  save,
  reset,
}: OpenIdVerifiableCredentialsProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <>
      <Text className="pf-v5-u-pb-lg">
        {t("openIdVerifiableCredentialsHelp")}
      </Text>
      <FormAccess role="manage-clients" isHorizontal>
        <DefaultSwitchControl
          name={convertAttributeNameToForm<FormFields>("attributes.oid4vci.enabled")}
          label={t("oid4vciEnabled")}
          labelIcon={t("oid4vciEnabledHelp")}
          stringify
        />
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
    </>
  );
};
