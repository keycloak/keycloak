import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ModalBox/modal-box';

export interface ModalBoxFooterProps {
  /** Content rendered inside the Footer */
  children?: React.ReactNode;
  /** Additional classes added to the Footer */
  className?: string;
}

export const ModalBoxFooter: React.FunctionComponent<ModalBoxFooterProps> = ({
  children = null,
  className = '',
  ...props
}: ModalBoxFooterProps) => (
  <footer {...props} className={css(styles.modalBoxFooter, className)}>
    {children}
  </footer>
);
ModalBoxFooter.displayName = 'ModalBoxFooter';
