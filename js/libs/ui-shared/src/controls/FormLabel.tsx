import {
  FormGroup,
  FormGroupProps,
  ValidatedOptions,
} from "@patternfly/react-core";
import { PropsWithChildren, ReactNode } from "react";
import { FieldError, FieldValues, Merge } from "react-hook-form";
import { HelpItem } from "./HelpItem";

const selectToggleTypeahead = "pf-c-select__toggle-typeahead";

export type FieldProps<T extends FieldValues = FieldValues> = {
  label?: string;
  name: string;
  labelIcon?: string | ReactNode;
  error?: FieldError | Merge<FieldError, T>;
  isRequired: boolean;
};

type FormLabelProps = FieldProps & Omit<FormGroupProps, "label" | "labelIcon">;

export const FormLabel = ({
  name,
  label,
  labelIcon,
  error,
  children,
  ...rest
}: PropsWithChildren<FormLabelProps>) => (
  <FormGroup
    onFocus={() => {
      const input = document.activeElement as HTMLInputElement;
      // Autofocus is currently limited to Option form elements where we are using
      // a typeahed control because there are more than 10 options
      if (input.classList.contains(selectToggleTypeahead) && input.value)
        input.select();
    }}
    label={label || name}
    fieldId={name}
    labelIcon={
      labelIcon ? (
        <HelpItem helpText={labelIcon} fieldLabelId={name} />
      ) : undefined
    }
    helperTextInvalid={error?.message}
    validated={error ? ValidatedOptions.error : ValidatedOptions.default}
    {...rest}
  >
    {children}
  </FormGroup>
);
