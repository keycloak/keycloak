import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import {
  FieldPath,
  FieldValues,
  PathValue,
  useController,
  UseControllerProps,
} from "react-hook-form";

import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";
import { HelpItem } from "./HelpItem";

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
    <FormGroup
      isRequired={required}
      label={props.label}
      labelIcon={
        props.labelIcon ? (
          <HelpItem helpText={props.labelIcon} fieldLabelId={props.name} />
        ) : undefined
      }
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
        isDisabled={props.isDisabled}
        {...field}
      />
    </FormGroup>
  );
};
