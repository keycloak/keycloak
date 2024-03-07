import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { FormGroup } from "@patternfly/react-core";
import { HelpItem, PasswordControl } from "ui-shared";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "ui-shared";

export const StoreSettings = ({
  hidePassword = false,
  isSaml = false,
}: {
  hidePassword?: boolean;
  isSaml?: boolean;
}) => {
  const { t } = useTranslation();
  const {
    register,
    formState: { errors },
  } = useFormContext<KeyStoreConfig>();

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
