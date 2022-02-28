import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Split/split';
import { getGutterModifier } from '../../styles/gutters';
import { css } from '@patternfly/react-styles';

export interface SplitProps extends React.HTMLProps<HTMLDivElement> {
  /** Adds space between children. */
  gutter?: 'sm' | 'md' | 'lg';
  /** content rendered inside the Split layout */
  children?: React.ReactNode;
  /** additional classes added to the Split layout */
  className?: string;
  /** Sets the base component to render. defaults to div */
  component?: React.ReactNode;
}

export const Split: React.FunctionComponent<SplitProps> = ({
  gutter = null,
  className = '',
  children = null,
  component = 'div',
  ...props
}: SplitProps) => {
  const Component = component as any;
  return (
    <Component
      {...props}
      className={css(styles.split, gutter && getGutterModifier(styles, gutter, styles.modifiers.gutter), className)}
    >
      {children}
    </Component>
  );
};
