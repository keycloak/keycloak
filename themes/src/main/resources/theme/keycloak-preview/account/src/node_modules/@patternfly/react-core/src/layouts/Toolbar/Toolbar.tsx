import * as React from 'react';
import { css } from '@patternfly/react-styles';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';

export interface ToolbarProps extends React.HTMLProps<HTMLDivElement> {
  /** Anything that can be rendered as toolbar content */
  children?: React.ReactNode;
  /** Classes applied to toolbar parent */
  className?: string;
}

export const Toolbar: React.FunctionComponent<ToolbarProps> = ({
  children = null,
  className = null,
  ...props
}: ToolbarProps) => (
  <div {...props} className={css('pf-l-toolbar', className)}>
    {children}
  </div>
);
