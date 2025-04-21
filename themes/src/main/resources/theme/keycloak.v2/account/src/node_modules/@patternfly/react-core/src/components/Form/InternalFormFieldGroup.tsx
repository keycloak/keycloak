import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { FormFieldGroupToggle } from './FormFieldGroupToggle';
import { GenerateId } from '../../helpers';

export interface InternalFormFieldGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
  /** Anything that can be rendered as form field group content. */
  children?: React.ReactNode;
  /** Additional classes added to the form field group. */
  className?: string;
  /** Form field group header */
  header?: any;
  /** Flag indicating if the field group is expandable */
  isExpandable?: boolean;
  /** Flag indicate if the form field group is expanded. Modifies the card to be expandable. */
  isExpanded?: boolean;
  /** Function callback called when user clicks toggle button */
  onToggle?: () => void;
  /** Aria-label to use on the form field group toggle button */
  toggleAriaLabel?: string;
}

export const InternalFormFieldGroup: React.FunctionComponent<InternalFormFieldGroupProps> = ({
  children,
  className,
  header,
  isExpandable,
  isExpanded,
  onToggle,
  toggleAriaLabel,
  ...props
}: InternalFormFieldGroupProps) => {
  const headerTitleText = header ? header.props.titleText : null;
  if (isExpandable && !toggleAriaLabel && !headerTitleText) {
    // eslint-disable-next-line no-console
    console.error(
      'FormFieldGroupExpandable:',
      'toggleAriaLabel or the titleText prop of FormFieldGroupHeader is required to make the toggle button accessible'
    );
  }
  return (
    <div
      className={css(styles.formFieldGroup, isExpanded && isExpandable && styles.modifiers.expanded, className)}
      role="group"
      {...(headerTitleText && { 'aria-labelledby': `${header.props.titleText.id}` })}
      {...props}
    >
      {isExpandable && (
        <GenerateId prefix="form-field-group-toggle">
          {id => (
            <FormFieldGroupToggle
              onToggle={onToggle}
              isExpanded={isExpanded}
              aria-label={toggleAriaLabel}
              toggleId={id}
              {...(headerTitleText && { 'aria-labelledby': `${header.props.titleText.id} ${id}` })}
            />
          )}
        </GenerateId>
      )}
      {header && header}
      {(!isExpandable || (isExpandable && isExpanded)) && (
        <div className={css(styles.formFieldGroupBody)}>{children}</div>
      )}
    </div>
  );
};
InternalFormFieldGroup.displayName = 'InternalFormFieldGroup';
