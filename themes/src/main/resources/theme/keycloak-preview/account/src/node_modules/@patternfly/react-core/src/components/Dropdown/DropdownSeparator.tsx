import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext, DropdownArrowContext } from './dropdownConstants';
import { InternalDropdownItem } from './InternalDropdownItem';

export interface SeparatorProps extends React.HTMLProps<HTMLAnchorElement> {
  /** Classes applied to root element of dropdown item */
  className?: string;
  /** Click event to pass to InternalDropdownItem */
  onClick?: (event: React.MouseEvent<HTMLAnchorElement> | React.KeyboardEvent | MouseEvent) => void;
}

export const DropdownSeparator: React.FunctionComponent<SeparatorProps> = ({
  className = '',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  ref, // Types of Ref are different for React.FC vs React.Component
  ...props
}: SeparatorProps) => (
  <DropdownContext.Consumer>
    {({ separatorClass }) => (
      <DropdownArrowContext.Consumer>
        {context => (
          <InternalDropdownItem
            {...props}
            context={context}
            className={css(separatorClass, className)}
            component="div"
            role="separator"
          />
        )}
      </DropdownArrowContext.Consumer>
    )}
  </DropdownContext.Consumer>
);
