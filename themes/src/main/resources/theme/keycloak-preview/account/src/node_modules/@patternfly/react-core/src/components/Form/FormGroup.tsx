import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { ASTERISK } from '../../helpers/htmlConstants';
import { FormContext } from './FormContext';
import { css, getModifier } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';

export interface FormGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
  /** Anything that can be rendered as FormGroup content. */
  children?: React.ReactNode;
  /** Additional classes added to the FormGroup. */
  className?: string;
  /** Label text before the field. */
  label?: React.ReactNode;
  /** Sets the FormGroup required. */
  isRequired?: boolean;
  /** Sets the FormGroup isValid. This prop will be deprecated. You should use validated instead. */
  isValid?: boolean;
  /** Sets the FormGroup validated. If you set to success, text color of helper text will be modified to indicate valid state.
   * If set to error,  text color of helper text will be modified to indicate error state.
   */
  validated?: 'success' | 'error' | 'default';
  /** Sets the FormGroup isInline. */
  isInline?: boolean;
  /** Helper text after the field. It can be a simple text or an object. */
  helperText?: React.ReactNode;
  /** Helper text after the field when the field is invalid. It can be a simple text or an object. */
  helperTextInvalid?: React.ReactNode;
  /** ID of the included field. It has to be the same for proper working. */
  fieldId: string;
}

export const FormGroup: React.FunctionComponent<FormGroupProps> = ({
  children = null,
  className = '',
  label,
  isRequired = false,
  isValid = true,
  validated = 'default',
  isInline = false,
  helperText,
  helperTextInvalid,
  fieldId,
  ...props
}: FormGroupProps) => {
  const validHelperText = (
    <div
      className={css(styles.formHelperText, validated === ValidatedOptions.success && styles.modifiers.success)}
      id={`${fieldId}-helper`}
      aria-live="polite"
    >
      {helperText}
    </div>
  );

  const inValidHelperText = (
    <div className={css(styles.formHelperText, styles.modifiers.error)} id={`${fieldId}-helper`} aria-live="polite">
      {helperTextInvalid}
    </div>
  );

  return (
    <FormContext.Consumer>
      {({ isHorizontal }: { isHorizontal: boolean }) => (
        <div
          {...props}
          className={css(styles.formGroup, isInline ? getModifier(styles, 'inline', className) : className)}
        >
          {label && (
            <label className={css(styles.formLabel)} htmlFor={fieldId}>
              <span className={css(styles.formLabelText)}>{label}</span>
              {isRequired && (
                <span className={css(styles.formLabelRequired)} aria-hidden="true">
                  {ASTERISK}
                </span>
              )}
            </label>
          )}
          {isHorizontal ? <div className={css(styles.formHorizontalGroup)}>{children}</div> : children}
          {(!isValid || validated === ValidatedOptions.error) && helperTextInvalid
            ? inValidHelperText
            : validated !== ValidatedOptions.error && helperText
            ? validHelperText
            : ''}
        </div>
      )}
    </FormContext.Consumer>
  );
};
