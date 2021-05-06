import React from "react";
import { useFormContext } from "react-hook-form";
import { TextInput } from "@patternfly/react-core";

import { FieldProps, FormGroupField } from "./FormGroupField";

export const TextField = ({ label, field, isReadOnly = false }: FieldProps) => {
  const { register } = useFormContext();
  return (
    <FormGroupField label={label}>
      <TextInput
        type="text"
        id={label}
        data-testid={label}
        name={field}
        ref={register}
        isReadOnly={isReadOnly}
      />
    </FormGroupField>
  );
};
