import * as React from 'react';
import { css } from '@patternfly/react-styles';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';

export interface ToolbarGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Anything that can be rendered as one toolbar group */
  children?: React.ReactNode;
  /** Classes applied to toolbar group */
  className?: string;
}

export const ToolbarGroup: React.FunctionComponent<ToolbarGroupProps> = ({
  children = null,
  className = null,
  ...props
}: ToolbarGroupProps) => (
  <div {...props} className={css('pf-l-toolbar__group', className)}>
    {children}
  </div>
);
