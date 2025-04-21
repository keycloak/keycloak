import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Card/card';

export interface CardActionsProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Card Action */
  children?: React.ReactNode;
  /** Additional classes added to the Action */
  className?: string;
  /** Flag indicating that the actions have no offset */
  hasNoOffset?: boolean;
}

export const CardActions: React.FunctionComponent<CardActionsProps> = ({
  children = null,
  className = '',
  hasNoOffset = false,
  ...props
}: CardActionsProps) => (
  <div className={css(styles.cardActions, hasNoOffset && styles.modifiers.noOffset, className)} {...props}>
    {children}
  </div>
);
CardActions.displayName = 'CardActions';
