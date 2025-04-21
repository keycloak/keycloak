import * as React from 'react';
import { DropdownGroup, DropdownGroupProps } from '../Dropdown';

export const ApplicationLauncherGroup: React.FunctionComponent<DropdownGroupProps> = ({
  children,
  ...props
}: DropdownGroupProps) => <DropdownGroup {...props}>{children}</DropdownGroup>;
ApplicationLauncherGroup.displayName = 'ApplicationLauncherGroup';
