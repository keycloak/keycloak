import { useFormContext } from "react-hook-form";

import { FieldProps, FormGroupField } from "./FormGroupField";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const TextField = ({ label, field, isReadOnly = false }: FieldProps) => {
  const { register } = useFormContext();
  return (
    <FormGroupField label={label}>
      <KeycloakTextInput
        id={label}
        data-testid={label}
        readOnlyVariant={isReadOnly ? "default" : undefined}
        {...register(field)}
      />
    </FormGroupField>
  );
};
