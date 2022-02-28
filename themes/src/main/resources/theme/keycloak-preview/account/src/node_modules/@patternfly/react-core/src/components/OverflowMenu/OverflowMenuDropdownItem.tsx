import * as React from 'react';
import { DropdownItem } from '../Dropdown';
import { OverflowMenuContext } from './OverflowMenuContext';

export interface OverflowMenuDropdownItemProps extends React.HTMLProps<HTMLDivElement> {
  /** Any elements that can be rendered in the menu */
  children?: any;
  /** Indicates when a dropdown item shows and hides the corresponding list item */
  isShared?: boolean;
}

export const OverflowMenuDropdownItem: React.SFC<OverflowMenuDropdownItemProps> = ({
  children,
  isShared = false
}: OverflowMenuDropdownItemProps) => (
  <OverflowMenuContext.Consumer>
    {value => (!isShared || value.isBelowBreakpoint) && <DropdownItem component="button"> {children} </DropdownItem>}
  </OverflowMenuContext.Consumer>
);
