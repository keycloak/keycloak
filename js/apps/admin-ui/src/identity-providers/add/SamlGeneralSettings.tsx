import {
  HelpItem,
  TextControl,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormattedLink } from "../../components/external-link/FormattedLink";
import { useRealm } from "../../context/realm-context/RealmContext";
import type { Environment } from "../../environment";
import { DisplayOrder } from "../component/DisplayOrder";
import { RedirectUrl } from "../component/RedirectUrl";

import "./saml-general-settings.css";

type SamlGeneralSettingsProps = {
  isAliasReadonly?: boolean;
};

export const SamlGeneralSettings = ({
  isAliasReadonly = false,
}: SamlGeneralSettingsProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { environment } = useEnvironment<Environment>();

  const { control } = useFormContext();
  const alias = useWatch({ control, name: "alias" });

  return (
    <>
      <RedirectUrl id={alias} />

      <TextControl
        name="alias"
        label={t("alias")}
        labelIcon={t("aliasHelp")}
        readOnly={isAliasReadonly}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl name="displayName" label={t("displayName")} />
      <DisplayOrder />
      {isAliasReadonly && (
        <FormGroup
          label={t("endpoints")}
          fieldId="endpoints"
          labelIcon={
            <HelpItem helpText={t("aliasHelp")} fieldLabelId="alias" />
          }
          className="keycloak__identity-providers__saml_link"
        >
          <FormattedLink
            title={t("samlEndpointsLabel")}
            href={`${environment.adminBaseUrl}/realms/${realm}/broker/${alias}/endpoint/descriptor`}
            isInline
          />
        </FormGroup>
      )}
    </>
  );
};
