import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/InputGroup/input-group';
import { css } from '@patternfly/react-styles';

export interface InputGroupTextProps extends React.HTMLProps<HTMLSpanElement | HTMLLabelElement> {
  /** Additional classes added to the input group text. */
  className?: string;
  /** Content rendered inside the input group text. */
  children: React.ReactNode;
  /** Component that wraps the input group text. */
  component?: React.ReactNode;
}

export const InputGroupText: React.FunctionComponent<InputGroupTextProps> = ({
  className = '',
  component = 'span',
  children,
  ...props
}: InputGroupTextProps) => {
  const Component = component as any;
  return (
    <Component className={css(styles.inputGroupText, className)} {...props}>
      {children}
    </Component>
  );
};
