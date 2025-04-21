/* eslint-disable no-console */
import * as React from 'react';
import { Button, ButtonProps, ButtonVariant } from '../../components/Button';
import { PageContextConsumer, PageContextProps } from './Page';

export interface PageToggleButtonProps extends ButtonProps {
  /** Content of the page toggle button */
  children?: React.ReactNode;
  /** True if the side nav is shown  */
  isNavOpen?: boolean;
  /** Callback function to handle the side nav toggle button, managed by the Page component if the Page isManagedSidebar prop is set to true */
  onNavToggle?: () => void;
}

export const PageToggleButton: React.FunctionComponent<PageToggleButtonProps> = ({
  children,
  isNavOpen = true,
  onNavToggle = () => undefined as any,
  ...props
}: PageToggleButtonProps) => (
  <PageContextConsumer>
    {({ isManagedSidebar, onNavToggle: managedOnNavToggle, isNavOpen: managedIsNavOpen }: PageContextProps) => {
      const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
      const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;

      return (
        <Button
          id="nav-toggle"
          onClick={navToggle}
          aria-label="Side navigation toggle"
          aria-expanded={navOpen ? 'true' : 'false'}
          variant={ButtonVariant.plain}
          {...props}
        >
          {children}
        </Button>
      );
    }}
  </PageContextConsumer>
);
PageToggleButton.displayName = 'PageToggleButton';
