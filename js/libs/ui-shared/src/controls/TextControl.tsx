import { ValidatedOptions } from "@patternfly/react-core";
import {
  FieldPath,
  FieldValues,
  PathValue,
  useController,
  UseControllerProps,
} from "react-hook-form";

import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";
import { FormLabel } from "./FormLabel";

export type TextControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
> = UseControllerProps<T, P> & {
  label: string;
  labelIcon?: string;
  isDisabled?: boolean;
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
    <FormLabel
      name={props.name}
      label={props.label}
      labelIcon={props.labelIcon}
      isRequired={required}
      error={fieldState.error}
    >
      <KeycloakTextInput
        isRequired={required}
        id={props.name}
        data-testid={props.name}
        validated={
          fieldState.error ? ValidatedOptions.error : ValidatedOptions.default
        }
        isDisabled={props.isDisabled}
        {...field}
      />
    </FormLabel>
  );
};
