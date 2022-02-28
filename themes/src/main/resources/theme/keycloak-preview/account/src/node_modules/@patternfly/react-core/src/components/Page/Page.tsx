import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import globalBreakpointMd from '@patternfly/react-tokens/dist/js/global_breakpoint_md';
import { debounce } from '../../helpers/util';

export enum PageLayouts {
  vertical = 'vertical',
  horizontal = 'horizontal'
}

const PageContext = React.createContext({});
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
  /** Skip to content component for the page */
  skipToContent?: React.ReactElement;
  /** Sets the value for role on the <main> element */
  role?: string;
  /** an id to use for the [role="main"] element */
  mainContainerId?: string;
  /**
   * If true, manages the sidebar open/close state and there is no need to pass the isNavOpen boolean into
   * the sidebar component or add a callback onNavToggle function into the PageHeader component
   */
  isManagedSidebar?: boolean;
  /**
   * If true, the managed sidebar is initially open for desktop view
   */
  defaultManagedSidebarIsOpen?: boolean;
  /**
   * Can add callback to be notified when resize occurs, for example to set the sidebar isNav prop to false for a width < 768px
   * Returns object { mobileView: boolean, windowSize: number }
   */
  onPageResize?: (object: any) => void;
  /** Breadcrumb component for the page */
  breadcrumb?: React.ReactNode;
  /** Accessible label, can be used to name main section */
  mainAriaLabel?: string;
}

export interface PageState {
  desktopIsNavOpen: boolean;
  mobileIsNavOpen: boolean;
  mobileView: boolean;
}

export class Page extends React.Component<PageProps, PageState> {
  static defaultProps: PageProps = {
    breadcrumb: null as React.ReactNode,
    children: null as React.ReactNode,
    className: '',
    header: null as React.ReactNode,
    sidebar: null as React.ReactNode,
    skipToContent: null as React.ReactElement,
    isManagedSidebar: false,
    defaultManagedSidebarIsOpen: true,
    onPageResize: (): void => null,
    mainContainerId: null as string,
    role: undefined as string
  };

  constructor(props: PageProps) {
    super(props);

    const { isManagedSidebar, defaultManagedSidebarIsOpen } = props;
    const managedSidebarOpen = !isManagedSidebar ? true : defaultManagedSidebarIsOpen;
    this.state = {
      desktopIsNavOpen: managedSidebarOpen,
      mobileIsNavOpen: false,
      mobileView: false
    };
  }

  componentDidMount() {
    const { isManagedSidebar, onPageResize } = this.props;
    if (isManagedSidebar || onPageResize) {
      window.addEventListener('resize', debounce(this.handleResize, 250));
      // Initial check if should be shown
      this.handleResize();
    }
  }

  componentWillUnmount() {
    const { isManagedSidebar, onPageResize } = this.props;
    if (isManagedSidebar || onPageResize) {
      window.removeEventListener('resize', debounce(this.handleResize, 250));
    }
  }

  handleResize = () => {
    const { onPageResize } = this.props;
    const windowSize = window.innerWidth;
    // eslint-disable-next-line radix
    const mobileView = windowSize < Number.parseInt(globalBreakpointMd.value, 10);
    if (onPageResize) {
      onPageResize({ mobileView, windowSize });
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    this.setState(prevState => ({
      mobileView
    }));
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
      className,
      children,
      header,
      sidebar,
      skipToContent,
      role,
      mainContainerId,
      isManagedSidebar,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      defaultManagedSidebarIsOpen,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onPageResize,
      mainAriaLabel,
      ...rest
    } = this.props;
    const { mobileView, mobileIsNavOpen, desktopIsNavOpen } = this.state;

    const context = {
      isManagedSidebar,
      onNavToggle: mobileView ? this.onNavToggleMobile : this.onNavToggleDesktop,
      isNavOpen: mobileView ? mobileIsNavOpen : desktopIsNavOpen
    };

    return (
      <PageContextProvider value={context}>
        <div {...rest} className={css(styles.page, className)}>
          {skipToContent}
          {header}
          {sidebar}
          <main
            role={role}
            id={mainContainerId}
            className={css(styles.pageMain)}
            tabIndex={-1}
            aria-label={mainAriaLabel}
          >
            {breadcrumb && <section className={css(styles.pageMainBreadcrumb)}>{breadcrumb}</section>}
            {children}
          </main>
        </div>
      </PageContextProvider>
    );
  }
}
