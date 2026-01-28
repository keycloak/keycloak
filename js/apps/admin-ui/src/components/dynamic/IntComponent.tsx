import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { NumberComponentProps } from "./components";

export const IntComponent = ({
  name,
  label,
  helpText,
  convertToName,
  ...props
}: NumberComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      type="number"
      pattern="\d*"
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
