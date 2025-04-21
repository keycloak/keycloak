import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ActionList/action-list';

export interface ActionListGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Children of the action list group */
  children?: React.ReactNode;
  /** Additional classes added to the action list group */
  className?: string;
}

export const ActionListGroup: React.FunctionComponent<ActionListGroupProps> = ({
  children,
  className = '',
  ...props
}: ActionListGroupProps) => (
  <div className={css(styles.actionListGroup, className)} {...props}>
    {children}
  </div>
);
ActionListGroup.displayName = 'ActionListGroup';
