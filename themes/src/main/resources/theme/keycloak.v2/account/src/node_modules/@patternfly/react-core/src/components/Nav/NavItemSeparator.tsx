import * as React from 'react';
import { Divider, DividerProps } from '../Divider';

export const NavItemSeparator: React.FunctionComponent<DividerProps> = ({
  component = 'li',
  ...props
}: DividerProps) => <Divider component={component} {...props} />;
NavItemSeparator.displayName = 'NavItemSeparator';
