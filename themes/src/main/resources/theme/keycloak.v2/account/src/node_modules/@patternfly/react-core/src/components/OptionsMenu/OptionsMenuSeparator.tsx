import * as React from 'react';
import { Divider, DividerProps } from '../Divider';

export const OptionsMenuSeparator: React.FunctionComponent<DividerProps> = ({
  component = 'li',
  ...props
}: DividerProps) => <Divider component={component} {...props} />;
OptionsMenuSeparator.displayName = 'OptionsMenuSeparator';
