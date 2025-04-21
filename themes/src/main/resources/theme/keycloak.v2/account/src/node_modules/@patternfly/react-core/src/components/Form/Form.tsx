import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';

export interface FormProps extends React.HTMLProps<HTMLFormElement> {
  /** Anything that can be rendered as Form content. */
  children?: React.ReactNode;
  /** Additional classes added to the Form. */
  className?: string;
  /** Sets the Form to horizontal. */
  isHorizontal?: boolean;
  /** Limits the max-width of the form. */
  isWidthLimited?: boolean;
  /** Sets a custom max-width for the form. */
  maxWidth?: string;
}

export const Form: React.FunctionComponent<FormProps> = ({
  children = null,
  className = '',
  isHorizontal = false,
  isWidthLimited = false,
  maxWidth = '',
  ...props
}: FormProps) => (
  <form
    noValidate
    {...(maxWidth && {
      style: {
        '--pf-c-form--m-limit-width--MaxWidth': maxWidth,
        ...props.style
      } as React.CSSProperties
    })}
    {...props}
    className={css(
      styles.form,
      isHorizontal && styles.modifiers.horizontal,
      (isWidthLimited || maxWidth) && styles.modifiers.limitWidth,
      className
    )}
  >
    {children}
  </form>
);
Form.displayName = 'Form';
