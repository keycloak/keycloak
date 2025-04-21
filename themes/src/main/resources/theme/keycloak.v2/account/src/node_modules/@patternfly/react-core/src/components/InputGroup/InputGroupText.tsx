import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/InputGroup/input-group';
import { css } from '@patternfly/react-styles';

export enum InputGroupTextVariant {
  default = 'default',
  plain = 'plain'
}

export interface InputGroupTextProps extends React.HTMLProps<HTMLSpanElement | HTMLLabelElement> {
  /** Additional classes added to the input group text. */
  className?: string;
  /** Content rendered inside the input group text. */
  children: React.ReactNode;
  /** Component that wraps the input group text. */
  component?: React.ReactNode;
  /** Input group text variant */
  variant?: InputGroupTextVariant | 'default' | 'plain';
}

export const InputGroupText: React.FunctionComponent<InputGroupTextProps> = ({
  className = '',
  component = 'span',
  children,
  variant = InputGroupTextVariant.default,
  ...props
}: InputGroupTextProps) => {
  const Component = component as any;
  return (
    <Component
      className={css(
        styles.inputGroupText,
        variant === InputGroupTextVariant.plain && styles.modifiers.plain,
        className
      )}
      {...props}
    >
      {children}
    </Component>
  );
};
InputGroupText.displayName = 'InputGroupText';
