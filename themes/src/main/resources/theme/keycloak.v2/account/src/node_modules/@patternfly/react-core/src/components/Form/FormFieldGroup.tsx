import * as React from 'react';
import { InternalFormFieldGroup } from './InternalFormFieldGroup';

export interface FormFieldGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
  /** Anything that can be rendered as form field group content. */
  children?: React.ReactNode;
  /** Additional classes added to the form field group. */
  className?: string;
  /** Form field group header */
  header?: React.ReactNode;
}

export const FormFieldGroup: React.FunctionComponent<FormFieldGroupProps> = ({
  children,
  className,
  header,
  ...props
}: FormFieldGroupProps) => (
  <InternalFormFieldGroup className={className} header={header} {...props}>
    {children}
  </InternalFormFieldGroup>
);
FormFieldGroup.displayName = 'FormFieldGroup';
