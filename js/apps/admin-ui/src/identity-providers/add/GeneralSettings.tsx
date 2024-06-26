import { ClientIdSecret } from "../component/ClientIdSecret";
import { DisplayOrder } from "../component/DisplayOrder";
import { RedirectUrl } from "../component/RedirectUrl";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { useFormContext, useWatch } from "react-hook-form";
import { useParams } from "react-router-dom";
import type { IdentityProviderParams } from "../routes/IdentityProvider";

type GeneralSettingsProps = {
  id: string;
  create?: boolean;
};

export const GeneralSettings = ({
  create = true,
  id,
}: GeneralSettingsProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const alias = useWatch({ control, name: "alias" });
  const { tab } = useParams<IdentityProviderParams>();

  return (
    <>
      <RedirectUrl id={alias ? alias : id} />

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
      <ClientIdSecret create={create} />
      <DisplayOrder />
    </>
  );
};
