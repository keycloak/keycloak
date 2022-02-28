import * as React from 'react';
import { DropdownSeparator, SeparatorProps } from '../Dropdown';

export const ApplicationLauncherSeparator: React.FunctionComponent<SeparatorProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  children,
  ...props
}: SeparatorProps) => <DropdownSeparator {...props} />;
