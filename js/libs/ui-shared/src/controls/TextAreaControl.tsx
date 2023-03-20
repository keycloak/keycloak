import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import {
  FieldPath,
  FieldValues,
  PathValue,
  useController,
  UseControllerProps,
} from "react-hook-form";

import { HelpItem } from "./HelpItem";
import { KeycloakTextArea } from "./keycloak-text-area/KeycloakTextArea";

export type TextAreaControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
> = UseControllerProps<T, P> & {
  label: string;
  labelIcon?: string;
  isDisabled?: boolean;
};

export const TextAreaControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
>(
  props: TextAreaControlProps<T, P>
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
      <KeycloakTextArea
        isRequired={required}
        id={props.name}
        data-testid={props.name}
        validated={
          fieldState.error ? ValidatedOptions.error : ValidatedOptions.default
        }
        isDisabled={props.isDisabled}
        {...field}
      />
    </FormGroup>
  );
};
