import * as React from 'react';

export interface TabButtonProps extends Omit<React.HTMLProps<HTMLAnchorElement | HTMLButtonElement>, 'ref'> {
  /** content rendered inside the Tab content area. */
  children?: React.ReactNode;
  /** additional classes added to the Tab */
  className?: string;
  /** URL associated with the Tab. A Tab with an href will render as an <a> instead of a <button>. A Tab inside a <Tabs variant="nav"> should have an href. */
  href?: string;
  /** child reference for case in which a TabContent section is defined outside of a Tabs component */
  tabContentRef?: React.Ref<any>;
}

const TabButtonBase: React.FunctionComponent<TabButtonProps> = ({
  children,
  className = '',
  tabContentRef,
  ...props
}: TabButtonProps) => {
  const Component = (props.href ? 'a' : 'button') as any;
  return (
    <Component {...props} className={className} ref={tabContentRef}>
      {children}
    </Component>
  );
};

export const TabButton = React.forwardRef(
  (props: TabButtonProps, ref: React.Ref<HTMLAnchorElement | HTMLButtonElement>) => (
    <TabButtonBase {...props} tabContentRef={ref} />
  )
);

// export const TabButton = withForwardedRef(TabButtonWithRef);
