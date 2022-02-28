import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Level/level';
import { getGutterModifier } from '../../styles/gutters';

export interface LevelProps extends React.HTMLProps<HTMLDivElement> {
  /** Adds space between children. */
  gutter?: 'sm' | 'md' | 'lg';
  /** additional classes added to the Level layout */
  className?: string;
  /** content rendered inside the Level layout */
  children?: React.ReactNode;
}

export const Level: React.FunctionComponent<LevelProps> = ({
  gutter = null,
  className = '',
  children = null,
  ...props
}: LevelProps) => (
  <div
    {...props}
    className={css(styles.level, gutter && getGutterModifier(styles, gutter, styles.modifiers.gutter), className)}
  >
    {children}
  </div>
);
