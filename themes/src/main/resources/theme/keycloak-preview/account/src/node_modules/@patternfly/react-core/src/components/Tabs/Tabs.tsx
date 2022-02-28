import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';
import AngleLeftIcon from '@patternfly/react-icons/dist/js/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { getUniqueId, isElementInView, sideElementIsOutOfView } from '../../helpers/util';
import { SIDE } from '../../helpers/constants';
import { TabButton } from './TabButton';
import { TabContent } from './TabContent';
import { InjectedOuiaProps, withOuiaContext } from '../withOuia';

export enum TabsVariant {
  div = 'div',
  nav = 'nav'
}

export interface TabsProps extends Omit<React.HTMLProps<HTMLElement | HTMLDivElement>, 'onSelect'> {
  /** content rendered inside the Tabs Component. */
  children: React.ReactNode;
  /** additional classes added to the Tabs */
  className?: string;
  /** the index of the active tab */
  activeKey?: number | string;
  /** handle tab selection */
  onSelect?: (event: React.MouseEvent<HTMLElement, MouseEvent>, eventKey: number | string) => void;
  /** uniquely identifies the Tabs */
  id?: string;
  /** enables the filled tab list layout */
  isFilled?: boolean;
  /** enables Secondary Tab styling */
  isSecondary?: boolean;
  /** aria-label for the left Scroll Button */
  leftScrollAriaLabel?: string;
  /** aria-label for the right Scroll Button */
  rightScrollAriaLabel?: string;
  /** determines what tag is used around the Tabs. Use "nav" to define the Tabs inside a navigation region */
  variant?: 'div' | 'nav';
  /** provides an accessible label for the Tabs. Labels should be unique for each set of Tabs that are present on a page. When variant is set to nav, this prop should be defined to differentiate the Tabs from other navigation regions on the page. */
  'aria-label'?: string;
  /** waits until the first "enter" transition to mount tab children (add them to the DOM) */
  mountOnEnter?: boolean;
  /** unmounts tab children (removes them from the DOM) when they are no longer visible */
  unmountOnExit?: boolean;
}

export interface TabsState {
  showLeftScrollButton: boolean;
  showRightScrollButton: boolean;
  highlightLeftScrollButton: boolean;
  highlightRightScrollButton: boolean;
  shownKeys: (string | number)[];
}

class Tabs extends React.Component<TabsProps & InjectedOuiaProps, TabsState> {
  tabList = React.createRef<HTMLUListElement>();
  constructor(props: TabsProps & InjectedOuiaProps) {
    super(props);
    this.state = {
      showLeftScrollButton: false,
      showRightScrollButton: false,
      highlightLeftScrollButton: false,
      highlightRightScrollButton: false,
      shownKeys: [this.props.activeKey] // only for mountOnEnter case
    };
  }

  static defaultProps: PickOptional<TabsProps> = {
    className: '',
    activeKey: 0,
    onSelect: () => undefined as any,
    isFilled: false,
    isSecondary: false,
    leftScrollAriaLabel: 'Scroll left',
    rightScrollAriaLabel: 'Scroll right',
    variant: TabsVariant.div,
    mountOnEnter: false,
    unmountOnExit: false
  };

  handleTabClick(
    event: React.MouseEvent<HTMLElement, MouseEvent>,
    eventKey: number,
    tabContentRef: React.RefObject<any>,
    mountOnEnter: boolean
  ) {
    const { shownKeys } = this.state;
    this.props.onSelect(event, eventKey);
    // process any tab content sections outside of the component
    if (tabContentRef) {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      React.Children.map(this.props.children, (child: any, i) => {
        child.props.tabContentRef.current.hidden = true;
      });
      // most recently selected tabContent
      tabContentRef.current.hidden = false;
    }
    // Update scroll button state and which button to highlight
    setTimeout(() => {
      this.handleScrollButtons();
    }, 1);
    if (mountOnEnter) {
      this.setState({
        shownKeys: shownKeys.concat(eventKey)
      });
    }
  }

  handleScrollButtons = () => {
    if (this.tabList.current) {
      const container = this.tabList.current;
      // get first element and check if it is in view
      const showLeftScrollButton = !isElementInView(container, container.firstChild as HTMLElement, false);

      // get lase element and check if it is in view
      const showRightScrollButton = !isElementInView(container, container.lastChild as HTMLElement, false);

      // determine if selected tab is out of view and apply styles
      let selectedTab;
      const childrenArr = Array.from(container.children);
      childrenArr.forEach((child: any) => {
        const { className } = child;
        if (className.search('pf-m-current') > 0) {
          selectedTab = child;
        }
      });

      const sideOutOfView = sideElementIsOutOfView(container, selectedTab);

      this.setState({
        showLeftScrollButton,
        showRightScrollButton,
        highlightLeftScrollButton: (sideOutOfView === SIDE.LEFT || sideOutOfView === SIDE.BOTH) && showLeftScrollButton,
        highlightRightScrollButton:
          (sideOutOfView === SIDE.RIGHT || sideOutOfView === SIDE.BOTH) && showRightScrollButton
      });
    }
  };

  scrollLeft = () => {
    // find first Element that is fully in view on the left, then scroll to the element before it
    if (this.tabList.current) {
      const container = this.tabList.current;
      const childrenArr = Array.from(container.children);
      let firstElementInView: any;
      let lastElementOutOfView: any;
      let i;
      for (i = 0; i < childrenArr.length && !firstElementInView; i++) {
        if (isElementInView(container, childrenArr[i] as HTMLElement, false)) {
          firstElementInView = childrenArr[i];
          lastElementOutOfView = childrenArr[i - 1];
        }
      }
      if (lastElementOutOfView) {
        container.scrollLeft -= lastElementOutOfView.scrollWidth;
      }
    }
  };

  scrollRight = () => {
    // find last Element that is fully in view on the right, then scroll to the element after it
    if (this.tabList.current) {
      const container = this.tabList.current as any;
      const childrenArr = Array.from(container.children);
      let lastElementInView: any;
      let firstElementOutOfView: any;
      for (let i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
        if (isElementInView(container, childrenArr[i] as HTMLElement, false)) {
          lastElementInView = childrenArr[i];
          firstElementOutOfView = childrenArr[i + 1];
        }
      }
      if (firstElementOutOfView) {
        container.scrollLeft += firstElementOutOfView.scrollWidth;
      }
    }
  };

  componentDidMount() {
    window.addEventListener('resize', this.handleScrollButtons, false);
    // call the handle resize function to check if scroll buttons should be shown
    this.handleScrollButtons();
  }

  componentWillUnmount() {
    document.removeEventListener('resize', this.handleScrollButtons, false);
  }

  render() {
    const {
      className,
      children,
      activeKey,
      id,
      isFilled,
      isSecondary,
      leftScrollAriaLabel,
      rightScrollAriaLabel,
      'aria-label': ariaLabel,
      variant,
      ouiaContext,
      ouiaId,
      mountOnEnter,
      unmountOnExit,
      ...props
    } = this.props;
    const {
      showLeftScrollButton,
      showRightScrollButton,
      highlightLeftScrollButton,
      highlightRightScrollButton,
      shownKeys
    } = this.state;

    const uniqueId = id || getUniqueId();
    const Component: any = variant === TabsVariant.nav ? 'nav' : 'div';

    return (
      <React.Fragment>
        <Component
          aria-label={ariaLabel}
          className={css(
            styles.tabs,
            isFilled && styles.modifiers.fill,
            isSecondary && styles.modifiers.tabsSecondary,
            showLeftScrollButton && styles.modifiers.start,
            showRightScrollButton && styles.modifiers.end,
            highlightLeftScrollButton && styles.modifiers.startCurrent,
            highlightRightScrollButton && styles.modifiers.endCurrent,
            className
          )}
          {...(ouiaContext.isOuia && {
            'data-ouia-component-type': 'Tabs',
            'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
          })}
          id={id && id}
          {...props}
        >
          <button
            className={css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary)}
            aria-label={leftScrollAriaLabel}
            onClick={this.scrollLeft}
          >
            <AngleLeftIcon />
          </button>
          <ul className={css(styles.tabsList)} ref={this.tabList} onScroll={this.handleScrollButtons}>
            {React.Children.map(children, (child: any, index) => {
              const { title, eventKey, tabContentRef, id: childId, tabContentId, ...rest } = child.props;
              return (
                <li
                  key={index}
                  className={css(styles.tabsItem, eventKey === activeKey && styles.modifiers.current, className)}
                >
                  <TabButton
                    className={css(styles.tabsButton)}
                    onClick={(event: any) => this.handleTabClick(event, eventKey, tabContentRef, mountOnEnter)}
                    id={`pf-tab-${eventKey}-${childId || uniqueId}`}
                    aria-controls={
                      tabContentId ? `${tabContentId}` : `pf-tab-section-${eventKey}-${childId || uniqueId}`
                    }
                    tabContentRef={tabContentRef}
                    {...rest}
                  >
                    {title}
                  </TabButton>
                </li>
              );
            })}
          </ul>
          <button
            className={css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary)}
            aria-label={rightScrollAriaLabel}
            onClick={this.scrollRight}
          >
            <AngleRightIcon />
          </button>
        </Component>
        {React.Children.map(children, (child: any, index) => {
          if (
            !child.props.children ||
            (unmountOnExit && child.props.eventKey !== activeKey) ||
            (mountOnEnter && shownKeys.indexOf(child.props.eventKey) === -1)
          ) {
            return null;
          } else {
            return <TabContent key={index} activeKey={activeKey} child={child} id={child.props.id || uniqueId} />;
          }
        })}
      </React.Fragment>
    );
  }
}

const TabsWithOuiaContext = withOuiaContext(Tabs);

export { TabsWithOuiaContext as Tabs };
