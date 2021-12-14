import React, { FunctionComponent } from "react";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";

export type FieldProps = { label: string; field: string; isReadOnly?: boolean };
export type FormGroupFieldProps = { label: string };

export const FormGroupField: FunctionComponent<FormGroupFieldProps> = ({
  label,
  children,
}) => {
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
