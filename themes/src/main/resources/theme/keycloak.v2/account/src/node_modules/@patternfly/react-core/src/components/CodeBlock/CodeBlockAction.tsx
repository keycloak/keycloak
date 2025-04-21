import * as React from 'react';
import { css } from '@patternfly/react-styles';

export interface CodeBlockActionProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the code block action */
  children?: React.ReactNode;
  /** Additional classes passed to the code block action */
  className?: string;
}

export const CodeBlockAction: React.FunctionComponent<CodeBlockActionProps> = ({
  children = null,
  className,
  ...props
}: CodeBlockActionProps) => (
  <div className={css('pf-c-code-block__actions-item', className)} {...props}>
    {children}
  </div>
);

CodeBlockAction.displayName = 'CodeBlockAction';
