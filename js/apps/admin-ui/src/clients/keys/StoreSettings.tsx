import { useTranslation } from "react-i18next";
import { PasswordControl, TextControl } from "@keycloak/keycloak-ui-shared";

export const StoreSettings = ({
  hidePassword = false,
  isSaml = false,
}: {
  hidePassword?: boolean;
  isSaml?: boolean;
}) => {
  const { t } = useTranslation();

  return (
    <>
      <TextControl
        name="keyAlias"
        label={t("keyAlias")}
        labelIcon={t("keyAliasHelp")}
        rules={{
          required: t("required"),
        }}
      />
      {!hidePassword && (
        <PasswordControl
          name="keyPassword"
          label={t("keyPassword")}
          labelIcon={t("keyPasswordHelp")}
          rules={{
            required: t("required"),
          }}
        />
      )}
      {isSaml && (
        <TextControl
          name="realmAlias"
          label={t("realmCertificateAlias")}
          labelIcon={t("realmCertificateAliasHelp")}
        />
      )}
      <PasswordControl
        name="storePassword"
        label={t("storePassword")}
        labelIcon={t("storePasswordHelp")}
        rules={{
          required: t("required"),
        }}
      />
    </>
  );
};
