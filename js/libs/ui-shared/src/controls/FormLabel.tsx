import { FormGroup, FormGroupProps } from "@patternfly/react-core";
import { PropsWithChildren, ReactNode } from "react";
import { FieldError, FieldValues, Merge } from "react-hook-form";
import { FormErrorText } from "./FormErrorText";
import { HelpItem } from "./HelpItem";

export type FieldProps<T extends FieldValues = FieldValues> = {
  id?: string | undefined;
  label?: string;
  name: string;
  labelHelp?: string | ReactNode;
  error?: FieldError | Merge<FieldError, T>;
  isRequired: boolean;
};

type FormLabelProps = FieldProps & Omit<FormGroupProps, "label" | "labelHelp">;

export const FormLabel = ({
  id,
  name,
  label,
  labelHelp,
  error,
  children,
  ...rest
}: PropsWithChildren<FormLabelProps>) => (
  <FormGroup
    label={label || name}
    fieldId={id || name}
    labelHelp={
      labelHelp ? (
        <HelpItem helpText={labelHelp} fieldLabelId={id || name} />
      ) : undefined
    }
    {...rest}
  >
    {children}
    {error && (
      <FormErrorText data-testid={`${name}-helper`} message={error.message} />
    )}
  </FormGroup>
);
