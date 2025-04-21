import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';

export interface MenuInputProps extends React.HTMLProps<HTMLElement> {
  /** Items within input */
  children?: React.ReactNode;
  /** Forwarded ref */
  innerRef?: React.Ref<any>;
}

export const MenuInput = React.forwardRef((props: MenuInputProps, ref: React.Ref<HTMLDivElement>) => (
  <div {...props} className={css(styles.menuSearch, props.className)} ref={ref} />
));
MenuInput.displayName = 'MenuInput';
