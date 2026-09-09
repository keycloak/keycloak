import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { MultiLineInput } from "../multi-line-input/MultiLineInput";
import type { ComponentProps } from "./components";

function convertDefaultValue(formValue?: any): string[] {
  if (Array.isArray(formValue)) {
    const values = formValue.filter(
      (value) => value !== null && value !== undefined,
    );
    return values.length > 0 ? values : [""];
  }

  return formValue !== null && formValue !== undefined ? [formValue] : [""];
}

export const MultiValuedStringComponent = ({
  name,
  label,
  defaultValue,
  helpText,
  stringify,
  required,
  isDisabled = false,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const fieldName = convertToName(name!);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <MultiLineInput
        aria-label={t(label!)}
        name={fieldName}
        isDisabled={isDisabled}
        isRequired={required}
        defaultValue={convertDefaultValue(defaultValue)}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: t(label!).toLowerCase(),
        })}
        stringify={stringify}
      />
    </FormGroup>
  );
};
