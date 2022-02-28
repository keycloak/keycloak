import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Form/form';

export interface FormHelperTextProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Helper Text Item */
  children?: React.ReactNode;
  /** Adds error styling to the Helper Text  * */
  isError?: boolean;
  /** Hides the helper text * */
  isHidden?: boolean;
  /** Additional classes added to the Helper Text Item  */
  className?: string;
}

export const FormHelperText: React.FunctionComponent<FormHelperTextProps> = ({
  children = null,
  isError = false,
  isHidden = true,
  className = '',
  ...props
}: FormHelperTextProps) => (
  <p
    className={css(
      styles.formHelperText,
      isError ? getModifier(styles, 'error') : '',
      isHidden ? getModifier(styles, 'hidden') : '',
      className
    )}
    {...props}
  >
    {children}
  </p>
);
