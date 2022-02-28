import * as React from 'react';
import { css } from '@patternfly/react-styles';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';

export interface ToolbarItemProps extends React.HTMLProps<HTMLDivElement> {
  /** Anything that can be rendered as toolbar item content */
  children?: React.ReactNode;
  /** Classes applied to toolbar item */
  className?: string;
}

export const ToolbarItem: React.FunctionComponent<ToolbarItemProps> = ({
  children = null,
  className = null,
  ...props
}: ToolbarItemProps) => (
  <div {...props} className={css('pf-l-toolbar__item', className)}>
    {children}
  </div>
);
