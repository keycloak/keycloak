import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';

export interface WizardNavProps {
  /** children should be WizardNavItem components */
  children?: any;
  /** aria-label applied to the nav element */
  ariaLabel?: string;
  /** Whether the nav is expanded */
  isOpen?: boolean;
  /** True to return the inner list without the wrapping nav element */
  returnList?: boolean;
}

export const WizardNav: React.FunctionComponent<WizardNavProps> = ({
  children,
  ariaLabel,
  isOpen = false,
  returnList = false
}: WizardNavProps) => {
  const innerList = <ol className={css(styles.wizardNavList)}>{children}</ol>;

  if (returnList) {
    return innerList;
  }

  return (
    <nav className={css(styles.wizardNav, isOpen && 'pf-m-expanded')} aria-label={ariaLabel}>
      <ol className={css(styles.wizardNavList)}>{children}</ol>
    </nav>
  );
};
