import * as React from 'react';
import PopoverBase from '../../helpers/PopoverBase/PopoverBase';
import { Instance as TippyInstance, Props as TippyProps } from 'tippy.js';
import { KEY_CODES } from '../../helpers/constants';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import '@patternfly/react-styles/css/components/Tooltip/tippy.css';
import '@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css';
import { css, getModifier } from '@patternfly/react-styles';
import { PopoverContent } from './PopoverContent';
import { PopoverBody } from './PopoverBody';
import { PopoverHeader } from './PopoverHeader';
import { PopoverFooter } from './PopoverFooter';
import { PopoverCloseButton } from './PopoverCloseButton';
import GenerateId from '../../helpers/GenerateId/GenerateId';
import popoverMaxWidth from '@patternfly/react-tokens/dist/js/c_popover_MaxWidth';
import { ReactElement } from 'react';
import { PickOptional } from '../../helpers/typeUtils';
// Can't use ES6 imports :(
// The types for it are also wrong, we should probably ditch this dependency.
import { FocusTrap } from '../../helpers';

export enum PopoverPosition {
  auto = 'auto',
  top = 'top',
  bottom = 'bottom',
  left = 'left',
  right = 'right'
}

export interface PopoverProps {
  /** Accessible label, required when header is not present */
  'aria-label'?: string;
  /** The element to append the popover to, defaults to body */
  appendTo?: Element | ((ref: Element) => Element);
  /** Body content */
  bodyContent: React.ReactNode;
  /** If enableFlip is true, the popover responds to this boundary */
  boundary?: 'scrollParent' | 'window' | 'viewport' | HTMLElement;
  /** The reference element to which the popover is relatively placed to */
  children: ReactElement<any>;
  /** Popover additional class */
  className?: string;
  /** Aria label for the Close button */
  closeBtnAriaLabel?: string;
  /** Distance of the popover to its target, defaults to 25 */
  distance?: number;
  /** If true, tries to keep the popover in view by flipping it if necessary */
  enableFlip?: boolean;
  /**
   * The desired position to flip the popover to if the initial position is not possible.
   * By setting this prop to 'flip' it attempts to flip the popover to the opposite side if there is no space.
   * You can also pass an array of positions that determines the flip order. It should contain the initial position
   * followed by alternative positions if that position is unavailable.
   * Example: Initial position is 'top'. Button with popover is in the top right corner. 'flipBehavior' is set to
   * ['top', 'right', 'left']. Since there is no space to the top, it checks if right is available. There's also no
   * space to the right, so it finally shows the popover on the left.
   */
  flipBehavior?: 'flip' | ('top' | 'bottom' | 'left' | 'right')[];
  /** Footer content */
  footerContent?: React.ReactNode;
  /** Header content, leave empty for no header */
  headerContent?: React.ReactNode;
  /** Hides the popover when a click occurs outside (only works if isVisible is not controlled by the user) */
  hideOnOutsideClick?: boolean;
  /**
   * True to show the popover programmatically. Used in conjunction with the shouldClose prop.
   * By default, the popover child element handles click events automatically. If you want to control this programmatically,
   * the popover will not auto-close if the Close button is clicked, ESC key is used, or if a click occurs outside the popover.
   * Instead, the consumer is responsible for closing the popover themselves by adding a callback listener for the shouldClose prop.
   */
  isVisible?: boolean;
  /** Minimum width of the popover (default 6.25rem) */
  minWidth?: string;
  /** Maximum width of the popover (default 18.75rem) */
  maxWidth?: string;
  /** Lifecycle function invoked when the popover has fully transitioned out. */
  onHidden?: (tip: TippyInstance) => void;
  /** Lifecycle function invoked when the popover begins to transition out. */
  onHide?: (tip: TippyInstance) => void;
  /** Lifecycle function invoked when the popover has been mounted to the DOM. */
  onMount?: (tip: TippyInstance) => void;
  /** Lifecycle function invoked when the popover begins to transition in. */
  onShow?: (tip: TippyInstance) => void;
  /** Lifecycle function invoked when the popover has fully transitioned in. */
  onShown?: (tip: TippyInstance) => void;
  /**
   * Popover position. Note: With 'enableFlip' set to true,
   * it will change the position if there is not enough space for the starting position.
   * The behavior of where it flips to can be controlled through the flipBehavior prop.
   */
  position?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
  /**
   * Callback function that is only invoked when isVisible is also controlled. Called when the popover Close button is
   * clicked or the ESC key is used
   */
  shouldClose?: (tip: TippyInstance) => void;
  /** z-index of the popover */
  zIndex?: number;
  /** additional Props to pass through to tippy.js */
  tippyProps?: Partial<TippyProps>;
}

export interface PopoverState {
  isOpen: boolean;
  focusTrapActive: boolean;
}

export class Popover extends React.Component<PopoverProps, PopoverState> {
  private tip: TippyInstance;
  static defaultProps: PickOptional<PopoverProps> = {
    position: 'top',
    enableFlip: true,
    className: '',
    isVisible: null as boolean,
    shouldClose: (): void => null,
    'aria-label': '',
    headerContent: null as typeof PopoverHeader,
    footerContent: null as typeof PopoverFooter,
    appendTo: () => document.body,
    hideOnOutsideClick: true,
    onHide: (): void => null,
    onHidden: (): void => null,
    onShow: (): void => null,
    onShown: (): void => null,
    onMount: (): void => null,
    zIndex: 9999,
    maxWidth: popoverMaxWidth && popoverMaxWidth.value,
    closeBtnAriaLabel: 'Close',
    distance: 25,
    boundary: 'window',
    // For every initial starting position, there are 3 escape positions
    flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
    tippyProps: {}
  };

  constructor(props: PopoverProps) {
    super(props);
    this.state = {
      isOpen: false,
      focusTrapActive: false
    };
  }

  hideOrNotify = () => {
    if (this.props.isVisible === null) {
      // Handle closing
      this.tip.hide();
    } else {
      // notify consumer
      this.props.shouldClose(this.tip);
    }
  };

  handleEscOrEnterKey = (event: KeyboardEvent) => {
    if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.tip.state.isVisible) {
      this.hideOrNotify();
    } else if (!this.state.isOpen && event.keyCode === KEY_CODES.ENTER) {
      this.setState({ focusTrapActive: true });
    }
  };

  componentDidMount() {
    document.addEventListener('keydown', this.handleEscOrEnterKey, false);
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleEscOrEnterKey, false);
  }

  storeTippyInstance = (tip: TippyInstance) => {
    if (this.props.minWidth) {
      tip.popperChildren.tooltip.style.minWidth = this.props.minWidth;
    }
    tip.popperChildren.tooltip.classList.add(styles.popover);
    this.tip = tip;
  };

  closePopover = () => {
    this.hideOrNotify();
    this.setState({ focusTrapActive: false });
  };

  hideAllPopovers = () => {
    document.querySelectorAll('.tippy-popper').forEach((popper: any) => {
      if (popper._tippy) {
        popper._tippy.hide();
      }
    });
  };

  onHide = (tip: TippyInstance) => {
    if (this.state.isOpen) {
      this.setState({ isOpen: false });
    }
    return this.props.onHide(tip);
  };

  onHidden = (tip: TippyInstance) => this.props.onHidden(tip);

  onMount = (tip: TippyInstance) => this.props.onMount(tip);

  onShow = (tip: TippyInstance) => {
    const { hideOnOutsideClick, isVisible, onShow } = this.props;
    // hide all other open popovers first if events are managed by us
    if (!hideOnOutsideClick && isVisible === null) {
      this.hideAllPopovers();
    }
    if (this.state.isOpen === false) {
      this.setState({ isOpen: true });
    }
    return onShow(tip);
  };

  onShown = (tip: TippyInstance) => this.props.onShown(tip);

  onContentMouseDown = () => {
    if (this.state.focusTrapActive) {
      this.setState({ focusTrapActive: false });
    }
  };

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const {
      position,
      enableFlip,
      children,
      className,
      'aria-label': ariaLabel,
      headerContent,
      bodyContent,
      footerContent,
      isVisible,
      shouldClose,
      appendTo,
      hideOnOutsideClick,
      onHide,
      onHidden,
      onShow,
      onShown,
      onMount,
      zIndex,
      minWidth,
      maxWidth,
      closeBtnAriaLabel,
      distance,
      boundary,
      flipBehavior,
      tippyProps,
      ...rest
    } = this.props;
    /* eslint-enable @typescript-eslint/no-unused-vars */

    if (!headerContent && !ariaLabel) {
      return new Error('aria-label is required when header is not used');
    }

    const content = this.state.isOpen ? (
      <GenerateId>
        {randomId => (
          <FocusTrap active={this.state.focusTrapActive} focusTrapOptions={{ clickOutsideDeactivates: true }}>
            <div
              className={css(!enableFlip && getModifier(styles, position, styles.modifiers.top), className)}
              role="dialog"
              aria-modal="true"
              aria-label={headerContent ? undefined : ariaLabel}
              aria-labelledby={headerContent ? `popover-${randomId}-header` : undefined}
              aria-describedby={`popover-${randomId}-body`}
              onMouseDown={this.onContentMouseDown}
              {...rest}
            >
              <PopoverContent>
                <PopoverCloseButton onClose={this.closePopover} aria-label={closeBtnAriaLabel} />
                {headerContent && <PopoverHeader id={`popover-${randomId}-header`}>{headerContent}</PopoverHeader>}
                <PopoverBody id={`popover-${randomId}-body`}>{bodyContent}</PopoverBody>
                {footerContent && <PopoverFooter>{footerContent}</PopoverFooter>}
              </PopoverContent>
            </div>
          </FocusTrap>
        )}
      </GenerateId>
    ) : (
      <></>
    );
    const handleEvents = isVisible === null;
    const shouldHideOnClick = () => {
      if (handleEvents) {
        if (hideOnOutsideClick === true) {
          return true;
        }
        return 'toggle';
      }
      return false;
    };
    return (
      <PopoverBase
        {...tippyProps}
        arrow
        onCreate={this.storeTippyInstance}
        maxWidth={maxWidth}
        zIndex={zIndex}
        appendTo={appendTo}
        content={content}
        lazy
        trigger={handleEvents ? 'click' : 'manual'}
        isVisible={isVisible}
        hideOnClick={shouldHideOnClick()}
        theme="pf-popover"
        interactive
        interactiveBorder={0}
        placement={position}
        distance={distance}
        flip={enableFlip}
        flipBehavior={flipBehavior}
        boundary={boundary}
        popperOptions={{
          modifiers: {
            preventOverflow: {
              enabled: enableFlip
            },
            hide: {
              enabled: enableFlip
            }
          }
        }}
        onHide={(tip: TippyInstance) => this.onHide(tip)}
        onHidden={(tip: TippyInstance) => this.onHidden(tip)}
        onShow={(tip: TippyInstance) => this.onShow(tip)}
        onShown={(tip: TippyInstance) => this.onShown(tip)}
        onMount={(tip: TippyInstance) => this.onMount(tip)}
      >
        {children}
      </PopoverBase>
    );
  }
}
