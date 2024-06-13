import { useTranslation } from "react-i18next";
import { PasswordControl, TextControl } from "@keycloak/keycloak-ui-shared";

export const ClientIdSecret = ({
  secretRequired = true,
  create = true,
}: {
  secretRequired?: boolean;
  create?: boolean;
}) => {
  const { t } = useTranslation();

  return (
    <>
      <TextControl
        name="config.clientId"
        label={t("clientId")}
        labelIcon={t("clientIdHelp")}
        rules={{
          required: t("required"),
        }}
      />
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
