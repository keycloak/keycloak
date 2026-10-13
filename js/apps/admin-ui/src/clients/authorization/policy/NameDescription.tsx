import { useTranslation } from "react-i18next";
import { TextAreaControl, TextControl } from "@keycloak/keycloak-ui-shared";
import { useIsAdminPermissionsClient } from "../../../utils/useIsAdminPermissionsClient";

type NameDescriptionProps = {
  isDisabled?: boolean;
  clientId?: string;
};

export const NameDescription = ({
  isDisabled,
  clientId,
}: NameDescriptionProps) => {
  const { t } = useTranslation();
  const isAdminPermissionsClient = useIsAdminPermissionsClient(clientId!);

  return (
    <>
      <TextControl
        name="name"
        label={t("name")}
        labelIcon={isAdminPermissionsClient ? t("permissionNameHelpText") : ""}
        rules={{ required: t("required") }}
        isDisabled={isDisabled}
      />
      <TextAreaControl
        name="description"
        label={t("description")}
        rules={{
          maxLength: { message: t("maxLength", { length: 255 }), value: 255 },
        }}
        isDisabled={isDisabled}
      />
    </>
  );
};
