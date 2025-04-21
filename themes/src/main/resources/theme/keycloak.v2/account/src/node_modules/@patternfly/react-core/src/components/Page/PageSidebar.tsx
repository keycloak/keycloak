import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import { PageContextConsumer } from './Page';

export interface PageSidebarProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the page sidebar */
  className?: string;
  /** Component to render the side navigation (e.g. <Nav /> */
  nav?: React.ReactNode;
  /**
   * If true, manages the sidebar open/close state and there is no need to pass the isNavOpen boolean into
   * the sidebar component or add a callback onNavToggle function into the PageHeader component
   */
  isManagedSidebar?: boolean;
  /** Programmatically manage if the side nav is shown, if isManagedSidebar is set to true in the Page component, this prop is managed */
  isNavOpen?: boolean;
  /** Indicates the color scheme of the sidebar */
  theme?: 'dark' | 'light';
}

export interface PageSidebarContextProps {
  isNavOpen: boolean;
}
export const pageSidebarContextDefaults: PageSidebarContextProps = {
  isNavOpen: true
};
export const PageSidebarContext = React.createContext<Partial<PageSidebarContextProps>>(pageSidebarContextDefaults);

export const PageSidebar: React.FunctionComponent<PageSidebarProps> = ({
  className = '',
  nav,
  isNavOpen = true,
  theme = 'dark',
  ...props
}: PageSidebarProps) => (
  <PageContextConsumer>
    {({ isManagedSidebar, isNavOpen: managedIsNavOpen }: PageSidebarProps) => {
      const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;

      return (
        <div
          id="page-sidebar"
          className={css(
            styles.pageSidebar,
            theme === 'light' && styles.modifiers.light,
            navOpen && styles.modifiers.expanded,
            !navOpen && styles.modifiers.collapsed,
            className
          )}
          aria-hidden={!navOpen}
          {...props}
        >
          <div className={styles.pageSidebarBody}>
            <PageSidebarContext.Provider value={{ isNavOpen: navOpen }}>{nav}</PageSidebarContext.Provider>
          </div>
        </div>
      );
    }}
  </PageContextConsumer>
);
PageSidebar.displayName = 'PageSidebar';
