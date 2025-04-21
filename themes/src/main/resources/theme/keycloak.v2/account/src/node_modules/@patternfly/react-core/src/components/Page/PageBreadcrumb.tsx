import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Page/page';

export interface PageBreadcrumbProps extends React.HTMLProps<HTMLElement> {
  /** Additional classes to apply to the PageBreadcrumb */
  className?: string;
  /** Content rendered inside of the PageBreadcrumb */
  children?: React.ReactNode;
  /** Limits the width of the breadcrumb */
  isWidthLimited?: boolean;
  /** Modifier indicating if the PageBreadcrumb is sticky to the top or bottom */
  sticky?: 'top' | 'bottom';
  /** Flag indicating if PageBreadcrumb should have a shadow at the top */
  hasShadowTop?: boolean;
  /** Flag indicating if PageBreadcrumb should have a shadow at the bottom */
  hasShadowBottom?: boolean;
  /** Flag indicating if the PageBreadcrumb has a scrolling overflow */
  hasOverflowScroll?: boolean;
}

export const PageBreadcrumb = ({
  className = '',
  children,
  isWidthLimited,
  sticky,
  hasShadowTop = false,
  hasShadowBottom = false,
  hasOverflowScroll = false,
  ...props
}: PageBreadcrumbProps) => (
  <section
    className={css(
      styles.pageMainBreadcrumb,
      isWidthLimited && styles.modifiers.limitWidth,
      sticky === 'top' && styles.modifiers.stickyTop,
      sticky === 'bottom' && styles.modifiers.stickyBottom,
      hasShadowTop && styles.modifiers.shadowTop,
      hasShadowBottom && styles.modifiers.shadowBottom,
      hasOverflowScroll && styles.modifiers.overflowScroll,
      className
    )}
    {...(hasOverflowScroll && { tabIndex: 0 })}
    {...props}
  >
    {isWidthLimited && <div className={css(styles.pageMainBody)}>{children}</div>}
    {!isWidthLimited && children}
  </section>
);
PageBreadcrumb.displayName = 'PageBreadcrumb';
