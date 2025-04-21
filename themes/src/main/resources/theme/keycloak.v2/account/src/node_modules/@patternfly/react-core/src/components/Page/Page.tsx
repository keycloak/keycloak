import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import globalBreakpointXl from '@patternfly/react-tokens/dist/esm/global_breakpoint_xl';
import { debounce, canUseDOM } from '../../helpers/util';
import { Drawer, DrawerContent, DrawerContentBody, DrawerPanelContent } from '../Drawer';
import { PageGroup, PageGroupProps } from './PageGroup';
import { getResizeObserver } from '../../helpers/resizeObserver';
import { getBreakpoint } from '../../helpers/util';

export enum PageLayouts {
  vertical = 'vertical',
  horizontal = 'horizontal'
}

export interface PageContextProps {
  isManagedSidebar: boolean;
  onNavToggle: () => void;
  isNavOpen: boolean;
  width: number;
  getBreakpoint: (width: number | null) => 'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
}
export const pageContextDefaults: PageContextProps = {
  isManagedSidebar: false,
  isNavOpen: false,
  onNavToggle: () => null,
  width: null,
  getBreakpoint
};
export const PageContext = React.createContext<PageContextProps>(pageContextDefaults);

export const PageContextProvider = PageContext.Provider;
export const PageContextConsumer = PageContext.Consumer;

export interface PageProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the main section of the page layout (e.g. <PageSection />) */
  children?: React.ReactNode;
  /** Additional classes added to the page layout */
  className?: string;
  /** Header component (e.g. <PageHeader />) */
  header?: React.ReactNode;
  /** Sidebar component for a side nav (e.g. <PageSidebar />) */
  sidebar?: React.ReactNode;
  /** Notification drawer component for an optional notification drawer (e.g. <NotificationDrawer />) */
  notificationDrawer?: React.ReactNode;
  /** Flag indicating Notification drawer in expanded */
  isNotificationDrawerExpanded?: boolean;
  /** Flag indicating if breadcrumb width should be limited */
  isBreadcrumbWidthLimited?: boolean;
  /** Callback when notification drawer panel is finished expanding. */
  onNotificationDrawerExpand?: () => void;
  /** Skip to content component for the page */
  skipToContent?: React.ReactElement;
  /** Sets the value for role on the <main> element */
  role?: string;
  /** an id to use for the [role="main"] element */
  mainContainerId?: string;
  /** tabIndex to use for the [role="main"] element, null to unset it */
  mainTabIndex?: number | null;
  /**
   * If true, manages the sidebar open/close state and there is no need to pass the isNavOpen boolean into
   * the sidebar component or add a callback onNavToggle function into the PageHeader component
   */
  isManagedSidebar?: boolean;
  /** Flag indicating if tertiary nav width should be limited */
  isTertiaryNavWidthLimited?: boolean;
  /**
   * If true, the managed sidebar is initially open for desktop view
   */
  defaultManagedSidebarIsOpen?: boolean;
  /**
   * Can add callback to be notified when resize occurs, for example to set the sidebar isNav prop to false for a width < 768px
   * Returns object { mobileView: boolean, windowSize: number }
   */
  onPageResize?: (object: any) => void;
  /**
   * The page resize observer uses the breakpoints returned from this function when adding the pf-m-breakpoint-[default|sm|md|lg|xl|2xl] class
   * You can override the default getBreakpoint function to return breakpoints at different sizes than the default
   * You can view the default getBreakpoint function here:
   * https://github.com/patternfly/patternfly-react/blob/main/packages/react-core/src/helpers/util.ts
   */
  getBreakpoint?: (width: number | null) => 'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  /** Breadcrumb component for the page */
  breadcrumb?: React.ReactNode;
  /** Tertiary nav component for the page */
  tertiaryNav?: React.ReactNode;
  /** Accessible label, can be used to name main section */
  mainAriaLabel?: string;
  /** Flag indicating if the tertiaryNav should be in a group */
  isTertiaryNavGrouped?: boolean;
  /** Flag indicating if the breadcrumb should be in a group */
  isBreadcrumbGrouped?: boolean;
  /** Additional content of the group */
  additionalGroupedContent?: React.ReactNode;
  /** Additional props of the group */
  groupProps?: PageGroupProps;
}

export interface PageState {
  desktopIsNavOpen: boolean;
  mobileIsNavOpen: boolean;
  mobileView: boolean;
  width: number;
}

export class Page extends React.Component<PageProps, PageState> {
  static displayName = 'Page';
  static defaultProps: PageProps = {
    isManagedSidebar: false,
    isBreadcrumbWidthLimited: false,
    defaultManagedSidebarIsOpen: true,
    onPageResize: (): void => null,
    mainTabIndex: -1,
    isNotificationDrawerExpanded: false,
    onNotificationDrawerExpand: () => null,
    getBreakpoint
  };
  mainRef = React.createRef<HTMLDivElement>();
  pageRef = React.createRef<HTMLDivElement>();
  observer: any = () => {};

  constructor(props: PageProps) {
    super(props);

    const { isManagedSidebar, defaultManagedSidebarIsOpen } = props;
    const managedSidebarOpen = !isManagedSidebar ? true : defaultManagedSidebarIsOpen;
    this.state = {
      desktopIsNavOpen: managedSidebarOpen,
      mobileIsNavOpen: false,
      mobileView: false,
      width: null
    };
  }

  componentDidMount() {
    const { isManagedSidebar, onPageResize } = this.props;
    if (isManagedSidebar || onPageResize) {
      this.observer = getResizeObserver(this.pageRef.current, this.handleResize);
      const currentRef = this.mainRef.current;
      if (currentRef) {
        currentRef.addEventListener('mousedown', this.handleMainClick);
        currentRef.addEventListener('touchstart', this.handleMainClick);
      }
      // Initial check if should be shown
      this.resize();
    }
  }

  componentWillUnmount() {
    const { isManagedSidebar, onPageResize } = this.props;
    if (isManagedSidebar || onPageResize) {
      this.observer();
      const currentRef = this.mainRef.current;
      if (currentRef) {
        currentRef.removeEventListener('mousedown', this.handleMainClick);
        currentRef.removeEventListener('touchstart', this.handleMainClick);
      }
    }
  }

  getWindowWidth = () => {
    if (canUseDOM) {
      return this.pageRef.current ? this.pageRef.current.clientWidth : window.innerWidth;
    } else {
      return 1200;
    }
  };

  isMobile = () =>
    // eslint-disable-next-line radix
    this.getWindowWidth() < Number.parseInt(globalBreakpointXl.value, 10);

  resize = () => {
    const { onPageResize } = this.props;
    const mobileView = this.isMobile();
    if (onPageResize) {
      onPageResize({ mobileView, windowSize: this.getWindowWidth() });
    }
    if (mobileView !== this.state.mobileView) {
      this.setState({ mobileView });
    }
    this.pageRef.current && this.setState({ width: this.pageRef.current.clientWidth });
  };

  handleResize = debounce(this.resize, 250);

  handleMainClick = () => {
    if (this.isMobile() && this.state.mobileIsNavOpen && this.mainRef.current) {
      this.setState({ mobileIsNavOpen: false });
    }
  };

  onNavToggleMobile = () => {
    this.setState(prevState => ({
      mobileIsNavOpen: !prevState.mobileIsNavOpen
    }));
  };

  onNavToggleDesktop = () => {
    this.setState(prevState => ({
      desktopIsNavOpen: !prevState.desktopIsNavOpen
    }));
  };

  render() {
    const {
      breadcrumb,
      isBreadcrumbWidthLimited,
      className,
      children,
      header,
      sidebar,
      notificationDrawer,
      isNotificationDrawerExpanded,
      onNotificationDrawerExpand,
      isTertiaryNavWidthLimited,
      skipToContent,
      role,
      mainContainerId,
      isManagedSidebar,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      defaultManagedSidebarIsOpen,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onPageResize,
      getBreakpoint,
      mainAriaLabel,
      mainTabIndex,
      tertiaryNav,
      isTertiaryNavGrouped,
      isBreadcrumbGrouped,
      additionalGroupedContent,
      groupProps,
      ...rest
    } = this.props;
    const { mobileView, mobileIsNavOpen, desktopIsNavOpen, width } = this.state;

    const context = {
      isManagedSidebar,
      onNavToggle: mobileView ? this.onNavToggleMobile : this.onNavToggleDesktop,
      isNavOpen: mobileView ? mobileIsNavOpen : desktopIsNavOpen,
      width,
      getBreakpoint
    };

    let nav = null;
    if (tertiaryNav && isTertiaryNavWidthLimited) {
      nav = (
        <div className={css(styles.pageMainNav, styles.modifiers.limitWidth)}>
          <div className={css(styles.pageMainBody)}>{tertiaryNav}</div>
        </div>
      );
    } else if (tertiaryNav) {
      nav = <div className={css(styles.pageMainNav)}>{tertiaryNav}</div>;
    }

    let crumb = null;
    if (breadcrumb && isBreadcrumbWidthLimited) {
      crumb = (
        <section className={css(styles.pageMainBreadcrumb, styles.modifiers.limitWidth)}>
          <div className={css(styles.pageMainBody)}>{breadcrumb}</div>
        </section>
      );
    } else if (breadcrumb) {
      crumb = <section className={css(styles.pageMainBreadcrumb)}>{breadcrumb}</section>;
    }

    const isGrouped = isTertiaryNavGrouped || isBreadcrumbGrouped || additionalGroupedContent;

    const group = isGrouped ? (
      <PageGroup {...groupProps}>
        {isTertiaryNavGrouped && nav}
        {isBreadcrumbGrouped && crumb}
        {additionalGroupedContent}
      </PageGroup>
    ) : null;

    const main = (
      <main
        ref={this.mainRef}
        role={role}
        id={mainContainerId}
        className={css(styles.pageMain)}
        tabIndex={mainTabIndex}
        aria-label={mainAriaLabel}
      >
        {group}
        {!isTertiaryNavGrouped && nav}
        {!isBreadcrumbGrouped && crumb}
        {children}
      </main>
    );

    const panelContent = <DrawerPanelContent>{notificationDrawer}</DrawerPanelContent>;

    return (
      <PageContextProvider value={context}>
        <div
          ref={this.pageRef}
          {...rest}
          className={css(
            styles.page,
            width !== null && 'pf-m-resize-observer',
            width !== null && `pf-m-breakpoint-${getBreakpoint(width)}`,
            className
          )}
        >
          {skipToContent}
          {header}
          {sidebar}
          {notificationDrawer && (
            <div className={css(styles.pageDrawer)}>
              <Drawer isExpanded={isNotificationDrawerExpanded} onExpand={onNotificationDrawerExpand}>
                <DrawerContent panelContent={panelContent}>
                  <DrawerContentBody>{main}</DrawerContentBody>
                </DrawerContent>
              </Drawer>
            </div>
          )}
          {!notificationDrawer && main}
        </div>
      </PageContextProvider>
    );
  }
}
