import * as React from 'react';

export interface CardHeaderMainProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Card Head Main */
  children?: React.ReactNode;
  /** Additional classes added to the Card Head Main */
  className?: string;
}

export const CardHeaderMain: React.FunctionComponent<CardHeaderMainProps> = ({
  children = null,
  className = '',
  ...props
}: CardHeaderMainProps) => (
  <div className={className} {...props}>
    {children}
  </div>
);
CardHeaderMain.displayName = 'CardHeaderMain';
