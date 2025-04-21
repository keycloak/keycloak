import * as React from 'react';
import { css } from '@patternfly/react-styles';

export enum TextListItemVariants {
  li = 'li',
  dt = 'dt',
  dd = 'dd'
}

export interface TextListItemProps extends React.HTMLProps<HTMLElement> {
  /** Content rendered within the TextListItem */
  children?: React.ReactNode;
  /** Additional classes added to the TextListItem */
  className?: string;
  /** The text list item component */
  component?: 'li' | 'dt' | 'dd';
}

export const TextListItem: React.FunctionComponent<TextListItemProps> = ({
  children = null,
  className = '',
  component = TextListItemVariants.li,
  ...props
}: TextListItemProps) => {
  const Component: any = component;

  return (
    <Component {...props} data-pf-content className={css(className)}>
      {children}
    </Component>
  );
};
TextListItem.displayName = 'TextListItem';
