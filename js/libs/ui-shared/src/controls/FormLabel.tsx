import {
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";
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
  error,
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
    {...rest}
  >
    {children}
    {error?.message && (
      <FormHelperText>
        <HelperText>
          <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
            {error.message}
          </HelperTextItem>
        </HelperText>
      </FormHelperText>
    )}
  </FormGroup>
);
