import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import { Button, ButtonVariant } from '../../components/Button';
import { PageContextConsumer } from './Page';

export interface PageHeaderProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the page header */
  className?: string;
  /** Component to render the logo/brand (e.g. <Brand />) */
  logo?: React.ReactNode;
  /** Additional props passed to the logo anchor container */
  logoProps?: object;
  /** Component to use to wrap the passed <logo> */
  logoComponent?: React.ReactNode;
  /** Component to render the toolbar (e.g. <Toolbar />) */
  toolbar?: React.ReactNode;
  /** Component to render the avatar (e.g. <Avatar /> */
  avatar?: React.ReactNode;
  /** Component to render navigation within the header (e.g. <Nav /> */
  topNav?: React.ReactNode;
  /** True to show the nav toggle button (toggles side nav) */
  showNavToggle?: boolean;
  /** True if the side nav is shown  */
  isNavOpen?: boolean;
  /**
   * If true, manages the sidebar open/close state and there is no need to pass the isNavOpen boolean into
   * the sidebar component or add a callback onNavToggle function into the PageHeader component
   */
  isManagedSidebar?: boolean;
  /** Sets the value for role on the <main> element */
  role?: string;
  /** Callback function to handle the side nav toggle button, managed by the Page component if the Page isManagedSidebar prop is set to true */
  onNavToggle?: () => void;
  /** Aria Label for the nav toggle button */
  'aria-label'?: string;
}

export const PageHeader = ({
  className = '',
  logo = null as React.ReactNode,
  logoProps = null as object,
  logoComponent = 'a',
  toolbar = null as React.ReactNode,
  avatar = null as React.ReactNode,
  topNav = null as React.ReactNode,
  isNavOpen = true,
  role = undefined as string,
  showNavToggle = false,
  onNavToggle = () => undefined as any,
  'aria-label': ariaLabel = 'Global navigation',
  ...props
}: PageHeaderProps) => {
  const LogoComponent = logoComponent as any;
  return (
    <PageContextConsumer>
      {({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }: PageHeaderProps) => {
        const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
        const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;

        return (
          <header role={role} className={css(styles.pageHeader, className)} {...props}>
            {(showNavToggle || logo) && (
              <div className={css(styles.pageHeaderBrand)}>
                {showNavToggle && (
                  <div className={css(styles.pageHeaderBrandToggle)}>
                    <Button
                      id="nav-toggle"
                      onClick={navToggle}
                      aria-label={ariaLabel}
                      aria-controls="page-sidebar"
                      aria-expanded={navOpen ? 'true' : 'false'}
                      variant={ButtonVariant.plain}
                    >
                      <BarsIcon />
                    </Button>
                  </div>
                )}
                {logo && (
                  <LogoComponent className={css(styles.pageHeaderBrandLink)} {...logoProps}>
                    {logo}
                  </LogoComponent>
                )}
              </div>
            )}
            {/* Hide for now until we have the context selector component */}
            {/* <div className={css(styles.pageHeaderSelector)}>
            pf-c-context-selector
          </div> */}
            {topNav && <div className={css(styles.pageHeaderNav)}>{topNav}</div>}
            {(toolbar || avatar) && (
              <div className={css(styles.pageHeaderTools)}>
                {toolbar}
                {avatar}
              </div>
            )}
          </header>
        );
      }}
    </PageContextConsumer>
  );
};
