import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';

export interface EmptyStatePrimaryProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the EmptyStatePrimary */
  className?: string;
  /** Content rendered inside the EmptyStatePrimary */
  children: React.ReactNode;
}

export const EmptyStatePrimary: React.FunctionComponent<EmptyStatePrimaryProps> = ({
  children,
  className = '',
  ...props
}: EmptyStatePrimaryProps) => (
  <div className={css(styles.emptyStatePrimary, className)} {...props}>
    {children}
  </div>
);
EmptyStatePrimary.displayName = 'EmptyStatePrimary';
