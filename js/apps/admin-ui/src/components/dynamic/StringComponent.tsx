import { FormGroup, TextInput } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
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
  const { register } = useFormContext();

  return (
    <FormGroup
      style={{ display: isHidden ? 'none' : undefined }} // TIDECLOAK IMPLEMENTATION
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />} // TIDECLOAK IMPLEMENTATION
      fieldId={name!}
      isRequired={required}
    >
      <TextInput
        id={convertToName(name!)}
        data-testid={name}
        isDisabled={isDisabled} // TIDECLOAK IMPLEMENTATION
        defaultValue={defaultValue?.toString()} // TIDECLOAK IMPLEMENTATION
        {...register(convertToName(name!))}
      />
    </FormGroup>

  );
};
