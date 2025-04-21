import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Stack/stack';
import { css } from '@patternfly/react-styles';

export interface StackProps extends React.HTMLProps<HTMLDivElement> {
  /** Adds space between children. */
  hasGutter?: boolean;
  /** content rendered inside the Stack layout */
  children?: React.ReactNode;
  /** additional classes added to the Stack layout */
  className?: string;
  /** Sets the base component to render. defaults to div */
  component?: React.ReactNode;
}

export const Stack: React.FunctionComponent<StackProps> = ({
  hasGutter = false,
  className = '',
  children = null,
  component = 'div',
  ...props
}: StackProps) => {
  const Component = component as any;
  return (
    <Component {...props} className={css(styles.stack, hasGutter && styles.modifiers.gutter, className)}>
      {children}
    </Component>
  );
};
Stack.displayName = 'Stack';
