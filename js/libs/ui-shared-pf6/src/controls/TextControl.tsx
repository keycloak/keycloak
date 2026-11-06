import {
  FormHelperText,
  HelperText,
  HelperTextItem,
  TextInput,
  TextInputProps,
  ValidatedOptions,
} from "@patternfly/react-core";
import { ReactNode } from "react";
import {
  FieldPath,
  FieldValues,
  PathValue,
  UseControllerProps,
  useController,
} from "react-hook-form";
import { getRuleValue } from "../utils/getRuleValue";
import { FormLabel } from "./FormLabel";

export type TextControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = UseControllerProps<T, P> &
  Omit<TextInputProps, "name" | "isRequired" | "required"> & {
    label: string;
    labelIcon?: string | ReactNode;
    isDisabled?: boolean;
    helperText?: string;
    "data-testid"?: string;
    type?: string;
  };

export const TextControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>(
  props: TextControlProps<T, P>,
) => {
  const {
    labelIcon,
    helperText,
    label,
    name,
    rules,
    defaultValue: defaultValueProp,
    control,
    shouldUnregister,
    disabled,
    isDisabled,
    "data-testid": dataTestId,
    ...rest
  } = props;
  const required = !!getRuleValue(rules?.required);
  const defaultValue = defaultValueProp ?? ("" as PathValue<T, P>);

  const { field, fieldState } = useController({
    name,
    rules,
    defaultValue,
    control,
    shouldUnregister,
    disabled,
  });

  return (
    <FormLabel
      name={name}
      label={label}
      labelIcon={labelIcon}
      isRequired={required}
      error={fieldState.error}
    >
      <TextInput
        isRequired={required}
        id={name}
        data-testid={dataTestId || name}
        validated={
          fieldState.error ? ValidatedOptions.error : ValidatedOptions.default
        }
        isDisabled={isDisabled}
        {...rest}
        {...field}
      />
      {helperText && (
        <FormHelperText>
          <HelperText>
            <HelperTextItem>{helperText}</HelperTextItem>
          </HelperText>
        </FormHelperText>
      )}
    </FormLabel>
  );
};
