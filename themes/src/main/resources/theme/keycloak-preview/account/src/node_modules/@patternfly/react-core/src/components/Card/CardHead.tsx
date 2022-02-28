import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Card/card';

export interface CardHeadProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Card Head */
  children?: React.ReactNode;
  /** Additional classes added to the Head */
  className?: string;
}

export const CardHead: React.FunctionComponent<CardHeadProps> = ({
  children = null,
  className = '',
  ...props
}: CardHeadProps) => (
  <div className={css(styles.cardHead, className)} {...props}>
    {children}
  </div>
);
