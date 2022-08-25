import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "../help-enabler/HelpItem";
import { KeyValueInput } from "../key-value-form/KeyValueInput";
import { convertToName } from "./DynamicComponents";

export const MapComponent = ({ name, label, helpText }: ComponentProps) => {
  const { t } = useTranslation("dynamic");

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <KeyValueInput name={convertToName(name!)} />
    </FormGroup>
  );
};
