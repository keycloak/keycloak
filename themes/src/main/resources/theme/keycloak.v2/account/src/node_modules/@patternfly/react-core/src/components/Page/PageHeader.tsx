/* eslint-disable no-console */
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';
import { Button, ButtonVariant } from '../../components/Button';
import { PageContextConsumer, PageContextProps } from './Page';

export interface PageHeaderProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the page header */
  className?: string;
  /** Component to render the logo/brand, use <Brand /> */
  logo?: React.ReactNode;
  /** Additional props passed to the logo anchor container */
  logoProps?: object;
  /** Component to use to wrap the passed <logo> */
  logoComponent?: React.ReactNode;
  /** Component to render the header tools, use <PageHeaderTools />  */
  headerTools?: React.ReactNode;
  /** Component to render navigation within the header, use <Nav /> */
  topNav?: React.ReactNode;
  /** True to show the nav toggle button (toggles side nav) */
  showNavToggle?: boolean;
  /** True if the side nav is shown  */
  isNavOpen?: boolean;
  /** This prop is no longer managed through PageHeader but in the Page component. */
  isManagedSidebar?: boolean;
  /** Sets the value for role on the <main> element */
  role?: string;
  /** Callback function to handle the side nav toggle button, managed by the Page component if the Page isManagedSidebar prop is set to true */
  onNavToggle?: () => void;
  /** Aria Label for the nav toggle button */
  'aria-label'?: string;
}

export const PageHeader: React.FunctionComponent<PageHeaderProps> = ({
  className = '',
  logo = null as React.ReactNode,
  logoProps = null as object,
  logoComponent = 'a',
  headerTools = null as React.ReactNode,
  topNav = null as React.ReactNode,
  isNavOpen = true,
  isManagedSidebar: deprecatedIsManagedSidebar = undefined,
  role = undefined as string,
  showNavToggle = false,
  onNavToggle = () => undefined as any,
  'aria-label': ariaLabel = 'Global navigation',
  'aria-controls': ariaControls = null,
  ...props
}: PageHeaderProps) => {
  const LogoComponent = logoComponent as any;
  if ([false, true].includes(deprecatedIsManagedSidebar)) {
    console.warn(
      'isManagedSidebar is deprecated in the PageHeader component. To make the sidebar toggle uncontrolled, pass this prop in the Page component'
    );
  }
  return (
    <PageContextConsumer>
      {({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }: PageContextProps) => {
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
                      aria-controls={ariaControls}
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
            {topNav && <div className={css(styles.pageHeaderNav)}>{topNav}</div>}
            {headerTools}
          </header>
        );
      }}
    </PageContextConsumer>
  );
};
PageHeader.displayName = 'PageHeader';
