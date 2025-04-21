import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import { PickOptional } from '../../helpers/typeUtils';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';
import { getUniqueId, isElementInView, formatBreakpointMods } from '../../helpers/util';
import { TabContent } from './TabContent';
import { TabProps } from './Tab';
import { TabsContextProvider } from './TabsContext';
import { Button } from '../Button';
import { getOUIAProps, OUIAProps, getDefaultOUIAId, canUseDOM } from '../../helpers';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';

export enum TabsComponent {
  div = 'div',
  nav = 'nav'
}

export interface TabsProps extends Omit<React.HTMLProps<HTMLElement | HTMLDivElement>, 'onSelect'>, OUIAProps {
  /** Content rendered inside the tabs component. Must be React.ReactElement<TabProps>[] */
  children: React.ReactNode;
  /** Additional classes added to the tabs */
  className?: string;
  /** Tabs background color variant */
  variant?: 'default' | 'light300';
  /** The index of the active tab */
  activeKey?: number | string;
  /** The index of the default active tab. Set this for uncontrolled Tabs */
  defaultActiveKey?: number | string;
  /** Callback to handle tab selection */
  onSelect?: (event: React.MouseEvent<HTMLElement, MouseEvent>, eventKey: number | string) => void;
  /** @beta Callback to handle tab closing */
  onClose?: (event: React.MouseEvent<HTMLElement, MouseEvent>, eventKey: number | string) => void;
  /** @beta Callback for the add button. Passing this property inserts the add button */
  onAdd?: () => void;
  /** @beta Aria-label for the add button */
  addButtonAriaLabel?: string;
  /** Uniquely identifies the tabs */
  id?: string;
  /** Enables the filled tab list layout */
  isFilled?: boolean;
  /** Enables secondary tab styling */
  isSecondary?: boolean;
  /** Enables box styling to the tab component */
  isBox?: boolean;
  /** Enables vertical tab styling */
  isVertical?: boolean;
  /** Enables border bottom tab styling on tabs. Defaults to true. To remove the bottom border, set this prop to false. */
  hasBorderBottom?: boolean;
  /** Enables border bottom styling for secondary tabs */
  hasSecondaryBorderBottom?: boolean;
  /** Aria-label for the left scroll button */
  leftScrollAriaLabel?: string;
  /** Aria-label for the right scroll button */
  rightScrollAriaLabel?: string;
  /** Determines what tag is used around the tabs. Use "nav" to define the tabs inside a navigation region */
  component?: 'div' | 'nav';
  /** Provides an accessible label for the tabs. Labels should be unique for each set of tabs that are present on a page. When component is set to nav, this prop should be defined to differentiate the tabs from other navigation regions on the page. */
  'aria-label'?: string;
  /** Waits until the first "enter" transition to mount tab children (add them to the DOM) */
  mountOnEnter?: boolean;
  /** Unmounts tab children (removes them from the DOM) when they are no longer visible */
  unmountOnExit?: boolean;
  /** Flag indicates that the tabs should use page insets. */
  usePageInsets?: boolean;
  /** Insets at various breakpoints. */
  inset?: {
    default?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    sm?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    md?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    lg?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    xl?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    '2xl'?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
  };
  /** Enable expandable vertical tabs at various breakpoints. (isVertical should be set to true for this to work) */
  expandable?: {
    default?: 'expandable' | 'nonExpandable';
    sm?: 'expandable' | 'nonExpandable';
    md?: 'expandable' | 'nonExpandable';
    lg?: 'expandable' | 'nonExpandable';
    xl?: 'expandable' | 'nonExpandable';
    '2xl'?: 'expandable' | 'nonExpandable';
  };
  /** Flag to indicate if the vertical tabs are expanded */
  isExpanded?: boolean;
  /** Flag indicating the default expanded state for uncontrolled expand/collapse of */
  defaultIsExpanded?: boolean;
  /** Text that appears in the expandable toggle */
  toggleText?: string;
  /** Aria-label for the expandable toggle */
  toggleAriaLabel?: string;
  /** Callback function to toggle the expandable tabs. */
  onToggle?: (isExpanded: boolean) => void;
}

const variantStyle = {
  default: '',
  light300: styles.modifiers.colorSchemeLight_300
};

interface TabsState {
  showScrollButtons: boolean;
  disableLeftScrollButton: boolean;
  disableRightScrollButton: boolean;
  shownKeys: (string | number)[];
  uncontrolledActiveKey: number | string;
  uncontrolledIsExpandedLocal: boolean;
  ouiaStateId: string;
}

export class Tabs extends React.Component<TabsProps, TabsState> {
  static displayName = 'Tabs';
  tabList = React.createRef<HTMLUListElement>();
  constructor(props: TabsProps) {
    super(props);
    this.state = {
      showScrollButtons: false,
      disableLeftScrollButton: true,
      disableRightScrollButton: true,
      shownKeys: this.props.defaultActiveKey !== undefined ? [this.props.defaultActiveKey] : [this.props.activeKey], // only for mountOnEnter case
      uncontrolledActiveKey: this.props.defaultActiveKey,
      uncontrolledIsExpandedLocal: this.props.defaultIsExpanded,
      ouiaStateId: getDefaultOUIAId(Tabs.displayName)
    };

    if (this.props.isVertical && this.props.expandable !== undefined) {
      if (!this.props.toggleAriaLabel && !this.props.toggleText) {
        // eslint-disable-next-line no-console
        console.error(
          'Tabs:',
          'toggleAriaLabel or the toggleText prop is required to make the toggle button accessible'
        );
      }
    }
  }

  scrollTimeout: NodeJS.Timeout = null;

  static defaultProps: PickOptional<TabsProps> = {
    activeKey: 0,
    onSelect: () => undefined as any,
    isFilled: false,
    isSecondary: false,
    isVertical: false,
    isBox: false,
    hasBorderBottom: true,
    leftScrollAriaLabel: 'Scroll left',
    rightScrollAriaLabel: 'Scroll right',
    component: TabsComponent.div,
    mountOnEnter: false,
    unmountOnExit: false,
    ouiaSafe: true,
    variant: 'default',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle: (isExpanded): void => undefined
  };

  handleTabClick(
    event: React.MouseEvent<HTMLElement, MouseEvent>,
    eventKey: number | string,
    tabContentRef: React.RefObject<any>
  ) {
    const { shownKeys } = this.state;
    const { onSelect, defaultActiveKey } = this.props;
    // if defaultActiveKey Tabs are uncontrolled, set new active key internally
    if (defaultActiveKey !== undefined) {
      this.setState({
        uncontrolledActiveKey: eventKey
      });
    } else {
      onSelect(event, eventKey);
    }

    // process any tab content sections outside of the component
    if (tabContentRef) {
      React.Children.toArray(this.props.children)
        .map(child => child as React.ReactElement<TabProps>)
        .filter(child => child.props && child.props.tabContentRef && child.props.tabContentRef.current)
        .forEach(child => (child.props.tabContentRef.current.hidden = true));
      // most recently selected tabContent
      if (tabContentRef.current) {
        tabContentRef.current.hidden = false;
      }
    }
    if (this.props.mountOnEnter) {
      this.setState({
        shownKeys: shownKeys.concat(eventKey)
      });
    }
  }

  handleScrollButtons = () => {
    // add debounce to the scroll event
    clearTimeout(this.scrollTimeout);
    this.scrollTimeout = setTimeout(() => {
      const container = this.tabList.current;
      let disableLeftScrollButton = true;
      let disableRightScrollButton = true;
      let showScrollButtons = false;

      if (container && !this.props.isVertical) {
        // get first element and check if it is in view
        const overflowOnLeft = !isElementInView(container, container.firstChild as HTMLElement, false);

        // get last element and check if it is in view
        const overflowOnRight = !isElementInView(container, container.lastChild as HTMLElement, false);

        showScrollButtons = overflowOnLeft || overflowOnRight;

        disableLeftScrollButton = !overflowOnLeft;
        disableRightScrollButton = !overflowOnRight;
      }
      this.setState({
        showScrollButtons,
        disableLeftScrollButton,
        disableRightScrollButton
      });
    }, 100);
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
    if (!this.props.isVertical) {
      if (canUseDOM) {
        window.addEventListener('resize', this.handleScrollButtons, false);
      }
      // call the handle resize function to check if scroll buttons should be shown
      this.handleScrollButtons();
    }
  }

  componentWillUnmount() {
    if (!this.props.isVertical) {
      if (canUseDOM) {
        window.removeEventListener('resize', this.handleScrollButtons, false);
      }
    }
    clearTimeout(this.scrollTimeout);
  }

  componentDidUpdate(prevProps: TabsProps) {
    const { activeKey, mountOnEnter, children } = this.props;
    const { shownKeys } = this.state;
    if (prevProps.activeKey !== activeKey && mountOnEnter && shownKeys.indexOf(activeKey) < 0) {
      this.setState({
        shownKeys: shownKeys.concat(activeKey)
      });
    }

    if (
      prevProps.children &&
      children &&
      React.Children.toArray(prevProps.children).length !== React.Children.toArray(children).length
    ) {
      this.handleScrollButtons();
    }
  }

  render() {
    const {
      className,
      children,
      activeKey,
      defaultActiveKey,
      id,
      isFilled,
      isSecondary,
      isVertical,
      isBox,
      hasBorderBottom,
      hasSecondaryBorderBottom,
      leftScrollAriaLabel,
      rightScrollAriaLabel,
      'aria-label': ariaLabel,
      component,
      ouiaId,
      ouiaSafe,
      mountOnEnter,
      unmountOnExit,
      usePageInsets,
      inset,
      variant,
      expandable,
      isExpanded,
      defaultIsExpanded,
      toggleText,
      toggleAriaLabel,
      addButtonAriaLabel,
      onToggle,
      onClose,
      onAdd,
      ...props
    } = this.props;
    const {
      showScrollButtons,
      disableLeftScrollButton,
      disableRightScrollButton,
      shownKeys,
      uncontrolledActiveKey,
      uncontrolledIsExpandedLocal
    } = this.state;
    const filteredChildren = (React.Children.toArray(children) as React.ReactElement<TabProps>[])
      .filter(Boolean)
      .filter(child => !child.props.isHidden);

    const uniqueId = id || getUniqueId();
    const Component: any = component === TabsComponent.nav ? 'nav' : 'div';
    const localActiveKey = defaultActiveKey !== undefined ? uncontrolledActiveKey : activeKey;

    const isExpandedLocal = defaultIsExpanded !== undefined ? uncontrolledIsExpandedLocal : isExpanded;
    /*  Uncontrolled expandable tabs */
    const toggleTabs = (newValue: boolean) => {
      if (isExpanded === undefined) {
        this.setState({ uncontrolledIsExpandedLocal: newValue });
      } else {
        onToggle(newValue);
      }
    };

    return (
      <TabsContextProvider
        value={{
          variant,
          mountOnEnter,
          unmountOnExit,
          localActiveKey,
          uniqueId,
          handleTabClick: (...args) => this.handleTabClick(...args),
          handleTabClose: onClose
        }}
      >
        <Component
          aria-label={ariaLabel}
          className={css(
            styles.tabs,
            isFilled && styles.modifiers.fill,
            isSecondary && styles.modifiers.secondary,
            isVertical && styles.modifiers.vertical,
            isVertical && expandable && formatBreakpointMods(expandable, styles),
            isVertical && expandable && isExpandedLocal && styles.modifiers.expanded,
            isBox && styles.modifiers.box,
            showScrollButtons && !isVertical && styles.modifiers.scrollable,
            usePageInsets && styles.modifiers.pageInsets,
            !hasBorderBottom && styles.modifiers.noBorderBottom,
            hasSecondaryBorderBottom && styles.modifiers.borderBottom,
            formatBreakpointMods(inset, styles),
            variantStyle[variant],
            className
          )}
          {...getOUIAProps(Tabs.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
          id={id && id}
          {...props}
        >
          {expandable && isVertical && (
            <GenerateId>
              {randomId => (
                <div className={css(styles.tabsToggle)}>
                  <div className={css(styles.tabsToggleButton)}>
                    <Button
                      onClick={() => toggleTabs(!isExpandedLocal)}
                      variant="plain"
                      aria-label={toggleAriaLabel}
                      aria-expanded={isExpandedLocal}
                      id={`${randomId}-button`}
                      aria-labelledby={`${randomId}-text ${randomId}-button`}
                    >
                      <span className={css(styles.tabsToggleIcon)}>
                        <AngleRightIcon arian-hidden="true" />
                      </span>
                      {toggleText && (
                        <span className={css('pf-c-tabs__toggle-text')} id={`${randomId}-text`}>
                          {toggleText}
                        </span>
                      )}
                    </Button>
                  </div>
                </div>
              )}
            </GenerateId>
          )}
          <button
            className={css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary)}
            aria-label={leftScrollAriaLabel}
            onClick={this.scrollLeft}
            disabled={disableLeftScrollButton}
            aria-hidden={disableLeftScrollButton}
          >
            <AngleLeftIcon />
          </button>
          <ul className={css(styles.tabsList)} ref={this.tabList} onScroll={this.handleScrollButtons} role="tablist">
            {filteredChildren}
          </ul>
          <button
            className={css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary)}
            aria-label={rightScrollAriaLabel}
            onClick={this.scrollRight}
            disabled={disableRightScrollButton}
            aria-hidden={disableRightScrollButton}
          >
            <AngleRightIcon />
          </button>
          {onAdd !== undefined && (
            <span className={css(styles.tabsAdd)}>
              <Button variant="plain" aria-label={addButtonAriaLabel || 'Add tab'} onClick={onAdd}>
                <PlusIcon />
              </Button>
            </span>
          )}
        </Component>
        {filteredChildren
          .filter(
            child =>
              child.props.children &&
              !(unmountOnExit && child.props.eventKey !== localActiveKey) &&
              !(mountOnEnter && shownKeys.indexOf(child.props.eventKey) === -1)
          )
          .map(child => (
            <TabContent
              key={child.props.eventKey}
              activeKey={localActiveKey}
              child={child}
              id={child.props.id || uniqueId}
              ouiaId={child.props.ouiaId}
            />
          ))}
      </TabsContextProvider>
    );
  }
}
