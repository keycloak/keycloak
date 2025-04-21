import * as React from 'react';
import { css } from '@patternfly/react-styles';

export interface ActionListItemProps extends React.HTMLProps<HTMLDivElement> {
  /** Children of the action list item */
  children?: React.ReactNode;
  /** Additional classes added to the action list item */
  className?: string;
}

export const ActionListItem: React.FunctionComponent<ActionListItemProps> = ({
  children,
  className = '',
  ...props
}: ActionListItemProps) => (
  <div className={css('pf-c-action-list__item', className)} {...props}>
    {children}
  </div>
);
ActionListItem.displayName = 'ActionListItem';
