import { FormGroup } from "@patternfly/react-core";
import { PropsWithChildren } from "react";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";

export type FieldProps = { label: string; field: string; isReadOnly?: boolean };
export type FormGroupFieldProps = { label: string };

export const FormGroupField = ({
  label,
  children,
}: PropsWithChildren<FormGroupFieldProps>) => {
  const { t } = useTranslation("identity-providers");
  return (
    <FormGroup
      label={t(label)}
      fieldId={label}
      labelIcon={
        <HelpItem
          helpText={`identity-providers-help:${label}`}
          fieldLabelId={`identity-providers:${label}`}
        />
      }
    >
      {children}
    </FormGroup>
  );
};
