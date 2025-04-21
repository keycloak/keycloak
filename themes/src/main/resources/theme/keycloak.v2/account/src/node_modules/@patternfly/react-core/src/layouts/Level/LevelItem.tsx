import * as React from 'react';

export interface LevelItemProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Level Layout Item */
  children?: React.ReactNode;
}

export const LevelItem: React.FunctionComponent<LevelItemProps> = ({ children = null, ...props }: LevelItemProps) => (
  <div {...props}>{children}</div>
);
LevelItem.displayName = 'LevelItem';
