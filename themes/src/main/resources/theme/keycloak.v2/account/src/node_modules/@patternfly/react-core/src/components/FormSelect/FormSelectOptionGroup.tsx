import * as React from 'react';

export interface FormSelectOptionGroupProps extends Omit<React.HTMLProps<HTMLOptGroupElement>, 'disabled'> {
  /** content rendered inside the Select Option Group */
  children?: React.ReactNode;
  /** additional classes added to the Select Option */
  className?: string;
  /** the label for the option */
  label: string;
  /** flag indicating if the Option Group is disabled */
  isDisabled?: boolean;
}

export const FormSelectOptionGroup: React.FunctionComponent<FormSelectOptionGroupProps> = ({
  children = null,
  className = '',
  isDisabled = false,
  label,
  ...props
}: FormSelectOptionGroupProps) => (
  <optgroup {...props} disabled={!!isDisabled} className={className} label={label}>
    {children}
  </optgroup>
);
FormSelectOptionGroup.displayName = 'FormSelectOptionGroup';
