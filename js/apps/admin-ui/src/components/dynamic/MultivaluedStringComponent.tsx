import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { MultiLineInput } from "../multi-line-input/MultiLineInput";
import { convertToName } from "./DynamicComponents";

function convertDefaultValue(formValue?: any): string[] {
  return formValue && Array.isArray(formValue) ? formValue : [formValue];
}

export const MultiValuedStringComponent = ({
  name,
  label,
  defaultValue,
  helpText,
  stringify,
  required,
  isDisabled = false,
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
        defaultValue={convertDefaultValue(defaultValue)}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: t(label!).toLowerCase(),
        })}
        stringify={stringify}
      />
    </FormGroup>
  );
};
