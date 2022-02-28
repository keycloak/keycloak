import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';

export interface DropdownItemIconProps extends React.HTMLProps<HTMLAnchorElement> {
  /** Icon to be rendered in the dropdown item */
  children?: React.ReactNode;
  /** Classes applied to span element of dropdown icon item */
  className?: string;
}

export const DropdownItemIcon: React.FunctionComponent<DropdownItemIconProps> = ({
  children,
  className = '',
  ...props
}: DropdownItemIconProps) => (
  <span className={css(styles.dropdownMenuItemIcon, className)} {...props}>
    {children}
  </span>
);
