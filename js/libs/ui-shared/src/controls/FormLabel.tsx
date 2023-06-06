import {
  FormGroup,
  // ValidatedOptions
} from "@patternfly/react-core";
import { PropsWithChildren } from "react";
import { FieldError, FieldValues, Merge } from "react-hook-form";
import { HelpItem } from "./HelpItem";

export type FormLabelProps<T extends FieldValues = FieldValues> = {
  label?: string;
  name: string;
  labelIcon?: string;
  error?: FieldError | Merge<FieldError, T>;
  isRequired: boolean;
};

export const FormLabel = ({
  name,
  label,
  labelIcon,
  // error,
  children,
  ...rest
}: PropsWithChildren<FormLabelProps>) => (
  <FormGroup
    label={label || name}
    fieldId={name}
    labelIcon={
      labelIcon ? (
        <HelpItem helpText={labelIcon} fieldLabelId={name} />
      ) : undefined
    }
    // TODO: Use FormHelperText, HelperText, and HelperTextItem directly inside children. helperText, // helperTextInvalid and validated props have been removed.
    // helperTextInvalid={error?.message}
    // validated={error ? ValidatedOptions.error : ValidatedOptions.default}
    {...rest}
  >
    {children}
  </FormGroup>
);
