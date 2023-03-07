import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "ui-shared";
import { MultiLineInput } from "../multi-line-input/MultiLineInput";
import { convertToName } from "./DynamicComponents";

export const MultiValuedStringComponent = ({
  name,
  label,
  defaultValue,
  helpText,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const fieldName = convertToName(name!);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <MultiLineInput
        aria-label={t(label!)}
        name={fieldName}
        isDisabled={isDisabled}
        defaultValue={[defaultValue]}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: t(label!).toLowerCase(),
        })}
        stringify
      />
    </FormGroup>
  );
};
