import { useTranslation } from "react-i18next";
import { TextAreaControl, TextControl } from "@keycloak/keycloak-ui-shared";

type NameDescriptionProps = {
  isDisabled: boolean;
};

export const NameDescription = ({ isDisabled }: NameDescriptionProps) => {
  const { t } = useTranslation();

  return (
    <>
      <TextControl
        name="name"
        label={t("name")}
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
