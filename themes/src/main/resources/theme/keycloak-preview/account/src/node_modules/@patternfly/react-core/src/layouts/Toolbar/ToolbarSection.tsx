import * as React from 'react';
import { css } from '@patternfly/react-styles';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';

export interface ToolbarSectionProps extends React.HTMLProps<HTMLDivElement> {
  /** Anything that can be rendered as toolbar section */
  children?: React.ReactNode;
  /** Classes applied to toolbar section */
  className?: string;
  /** Aria label applied to toolbar section */
  'aria-label'?: string;
}

export const ToolbarSection: React.FunctionComponent<ToolbarSectionProps> = ({
  children = null,
  className = null,
  'aria-label': ariaLabel,
  ...props
}: ToolbarSectionProps) => {
  if (!ariaLabel) {
    throw new Error('ToolbarSection requires aria-label to be specified');
  }

  return (
    <section {...props} className={css('pf-l-toolbar__section', className)}>
      {children}
    </section>
  );
};
