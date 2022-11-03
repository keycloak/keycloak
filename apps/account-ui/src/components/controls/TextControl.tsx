import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import {
  FieldPath,
  FieldValues,
  PathValue,
  useController,
  UseControllerProps,
} from "react-hook-form";

import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";

type TextControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
> = UseControllerProps<T, P> & {
  label: string;
};

export const TextControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
>(
  props: TextControlProps<T, P>
) => {
  const required = !!props.rules?.required;
  const defaultValue = props.defaultValue ?? ("" as PathValue<T, P>);

  const { field, fieldState } = useController({
    ...props,
    defaultValue,
  });

  return (
    <FormGroup
      isRequired={required}
      label={props.label}
      fieldId={props.name}
      helperTextInvalid={fieldState.error?.message}
      validated={
        fieldState.error ? ValidatedOptions.error : ValidatedOptions.default
      }
    >
      <KeycloakTextInput
        isRequired={required}
        id={props.name}
        validated={
          fieldState.error ? ValidatedOptions.error : ValidatedOptions.default
        }
        {...field}
      />
    </FormGroup>
  );
};
