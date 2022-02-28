import * as React from 'react';
import { css } from '@patternfly/react-styles';

export interface TabContentProps extends Omit<React.HTMLProps<HTMLElement>, 'ref'> {
  /** content rendered inside the tab content area if used outside Tabs component */
  children?: any;
  /** Child to show in the content area */
  child?: React.ReactElement<any>;
  /** class of tab content area if used outside Tabs component */
  className?: string;
  /** Identifies the active Tab  */
  activeKey?: number | string;
  /** uniquely identifies the controlling Tab if used outside Tabs component */
  eventKey?: number | string;
  /** Callback for the section ref */
  innerRef?: React.Ref<any>;
  /** id passed from parent to identify the content section */
  id: string;
  /** title of controlling Tab if used outside Tabs component */
  'aria-label'?: string;
}

const TabContentBase: React.FC<TabContentProps> = ({
  id,
  activeKey,
  'aria-label': ariaLabel,
  child,
  children,
  className,
  eventKey,
  innerRef,
  ...props
}: TabContentProps) => {
  if (children || child) {
    let labelledBy: string;
    if (ariaLabel) {
      labelledBy = null;
    } else {
      labelledBy = children ? `pf-tab-${eventKey}-${id}` : `pf-tab-${child.props.eventKey}-${id}`;
    }

    return (
      <section
        ref={innerRef}
        hidden={children ? null : child.props.eventKey !== activeKey}
        className={children ? css('pf-c-tab-content', className) : css('pf-c-tab-content', child.props.className)}
        id={children ? id : `pf-tab-section-${child.props.eventKey}-${id}`}
        aria-label={ariaLabel}
        aria-labelledby={labelledBy}
        role="tabpanel"
        tabIndex={0}
        {...props}
      >
        {children || child.props.children}
      </section>
    );
  }
  return null;
};

export const TabContent = React.forwardRef((props: TabContentProps, ref: React.Ref<HTMLElement>) => (
  <TabContentBase {...props} innerRef={ref} />
));
