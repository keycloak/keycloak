import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import type { ComponentProps } from "./components";

export const StringComponent = ({
  name,
  label,
  helpText,
  convertToName,
  defaultValue, // TIDECLOAK IMPLEMENTATION
  isDisabled = false, // TIDECLOAK IMPLEMENTATION
  required, // TIDECLOAK IMPLEMENTATION
  isHidden = false, // TIDECLOAK IMPLEMENTATION
  ...props
}: ComponentProps) => {
  const { t } = useTranslation();

  return (
    <div style={{ display: isHidden ? 'none' : undefined }}>{/* TIDECLOAK IMPLEMENTATION */}
      <TextControl
        name={convertToName(name!)}
        label={t(label!)}
        labelIcon={t(helpText!)}
        data-testid={name}
        isDisabled={isDisabled} // TIDECLOAK IMPLEMENTATION
        defaultValue={defaultValue?.toString()} // TIDECLOAK IMPLEMENTATION
        rules={{
          required: {
            value: !!required,
            message: t("required"),
          },
        }}
        {...props}
      />
    </div>
  );
};
