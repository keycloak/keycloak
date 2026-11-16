import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import type { ComponentProps } from "./components";

export const StringComponent = ({
  name,
  label,
  helpText,
  convertToName,
  required,
  isDisabled,
}: ComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      label={t(label!)}
      labelIcon={t(helpText!)}
      data-testid={name}
      type="text"
      isDisabled={isDisabled}
      rules={{
        required: {
          value: !!required,
          message: t("required"),
        },
      }}
    />
  );
};
