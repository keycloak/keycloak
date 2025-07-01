import { FormGroup, FormGroupProps } from "@patternfly/react-core";
import { PropsWithChildren, ReactNode } from "react";
import { FieldError, FieldValues, Merge } from "react-hook-form";
import { FormErrorText } from "./FormErrorText";
import { HelpItem } from "./HelpItem";

export type FieldProps<T extends FieldValues = FieldValues> = {
  id?: string | undefined;
  label?: string;
  name: string;
  labelIcon?: string | ReactNode;
  error?: FieldError | Merge<FieldError, T>;
  isRequired: boolean;
};

type FormLabelProps = FieldProps & Omit<FormGroupProps, "label" | "labelIcon">;

export const FormLabel = ({
  id,
  name,
  label,
  labelIcon,
  error,
  children,
  ...rest
}: PropsWithChildren<FormLabelProps>) => (
  <FormGroup
    label={label || name}
    fieldId={id || name}
    labelIcon={
      labelIcon ? (
        <HelpItem helpText={labelIcon} fieldLabelId={id || name} />
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
