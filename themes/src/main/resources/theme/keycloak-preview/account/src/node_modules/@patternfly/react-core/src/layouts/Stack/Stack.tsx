import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Stack/stack';
import { css } from '@patternfly/react-styles';
import { getGutterModifier } from '../../styles/gutters';

export interface StackProps extends React.HTMLProps<HTMLDivElement> {
  /** Adds space between children. */
  gutter?: 'sm' | 'md' | 'lg';
  /** content rendered inside the Stack layout */
  children?: React.ReactNode;
  /** additional classes added to the Stack layout */
  className?: string;
  /** Sets the base component to render. defaults to div */
  component?: React.ReactNode;
}

export const Stack: React.FunctionComponent<StackProps> = ({
  gutter = null,
  className = '',
  children = null,
  component = 'div',
  ...props
}: StackProps) => {
  const Component = component as any;
  return (
    <Component
      {...props}
      className={css(styles.stack, gutter && getGutterModifier(styles, gutter, styles.modifiers.gutter), className)}
    >
      {children}
    </Component>
  );
};
