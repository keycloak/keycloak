import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { DisplayOrder } from "../component/DisplayOrder";
import { RedirectUrl } from "../component/RedirectUrl";
import type { IdentityProviderParams } from "../routes/IdentityProvider";

export const OIDCGeneralSettings = () => {
  const { t } = useTranslation();
  const { tab } = useParams<IdentityProviderParams>();

  const { control } = useFormContext();
  const alias = useWatch({ control, name: "alias" });

  return (
    <>
      <RedirectUrl id={alias} />

      <TextControl
        name="alias"
        label={t("alias")}
        labelIcon={t("aliasHelp")}
        readOnly={tab === "settings"}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl name="displayName" label={t("displayName")} />
      <DisplayOrder />
    </>
  );
};
