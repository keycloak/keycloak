import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';

export interface EmptyStateSecondaryActionsProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the EmptyState */
  children?: React.ReactNode;
  /** Additional classes added to the EmptyState */
  className?: string;
}
export const EmptyStateSecondaryActions: React.FunctionComponent<EmptyStateSecondaryActionsProps> = ({
  children = null,
  className = '',
  ...props
}: EmptyStateSecondaryActionsProps) => (
  <div className={css(styles.emptyStateSecondary, className)} {...props}>
    {children}
  </div>
);
EmptyStateSecondaryActions.displayName = 'EmptyStateSecondaryActions';
