import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import type { ComponentProps } from "./components";

export const StringComponent = ({
  name,
  label,
  helpText,
  convertToName,
  ...props
}: ComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      label={t(label!)}
      labelIcon={t(helpText!)}
      data-testid={name}
      rules={{
        required: {
          value: !!props.required,
          message: t("required"),
        },
      }}
      {...props}
    />
  );
};
