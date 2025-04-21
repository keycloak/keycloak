/* eslint-disable no-console */
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tooltip/tooltip';
import { css } from '@patternfly/react-styles';
import { TooltipContent } from './TooltipContent';
import { TooltipArrow } from './TooltipArrow';
import { KEY_CODES } from '../../helpers/constants';
import tooltipMaxWidth from '@patternfly/react-tokens/dist/esm/c_tooltip_MaxWidth';
import { ReactElement } from 'react';
import { Popper, getOpacityTransition } from '../../helpers/Popper/Popper';
import { Props as TippyProps } from '../../helpers/Popper/DeprecatedTippyTypes';

export enum TooltipPosition {
  auto = 'auto',
  top = 'top',
  bottom = 'bottom',
  left = 'left',
  right = 'right',
  topStart = 'top-start',
  topEnd = 'top-end',
  bottomStart = 'bottom-start',
  bottomEnd = 'bottom-end',
  leftStart = 'left-start',
  leftEnd = 'left-end',
  rightStart = 'right-start',
  rightEnd = 'right-end'
}

export interface TooltipProps extends Omit<React.HTMLProps<HTMLDivElement>, 'content'> {
  /** The element to append the tooltip to, defaults to body */
  appendTo?: HTMLElement | ((ref?: HTMLElement) => HTMLElement);
  /**
   * aria-labelledby or aria-describedby for tooltip.
   * The trigger will be cloned to add the aria attribute, and the corresponding id in the form of 'pf-tooltip-#' is added to the content container.
   * If you don't want that or prefer to add the aria attribute yourself on the trigger, set aria to 'none'.
   */
  aria?: 'describedby' | 'labelledby' | 'none';
  /**
   * Determines whether the tooltip is an aria-live region. If the reference prop is passed in the
   * default behavior is 'polite' in order to ensure the tooltip contents is announced to
   * assistive technologies. Otherwise the default behavior is 'off'.
   */
  'aria-live'?: 'off' | 'polite';
  /**
   * The reference element to which the Tooltip is relatively placed to.
   * If you cannot wrap the reference with the Tooltip, you can use the reference prop instead.
   * Usage: <Tooltip><Button>Reference</Button></Tooltip>
   */
  children?: ReactElement<any>;
  /**
   * The reference element to which the Tooltip is relatively placed to.
   * If you can wrap the reference with the Tooltip, you can use the children prop instead.
   * Usage: <Tooltip reference={() => document.getElementById('reference-element')} />
   */
  reference?: HTMLElement | (() => HTMLElement) | React.RefObject<any>;
  /** Tooltip additional class */
  className?: string;
  /** Tooltip content */
  content: React.ReactNode;
  /** Distance of the tooltip to its target, defaults to 15 */
  distance?: number;
  /** If true, tries to keep the tooltip in view by flipping it if necessary */
  enableFlip?: boolean;
  /** Delay in ms before the tooltip appears */
  entryDelay?: number;
  /** Delay in ms before the tooltip disappears, Avoid passing in a value of "0", as users should
   * be given ample time to move their mouse from the trigger to the tooltip content without the content
   * being hidden.
   */
  exitDelay?: number;
  /**
   * The desired position to flip the tooltip to if the initial position is not possible.
   * By setting this prop to 'flip' it attempts to flip the tooltip to the opposite side if there is no space.
   * You can also pass an array of positions that determines the flip order. It should contain the initial position
   * followed by alternative positions if that position is unavailable.
   * Example: Initial position is 'top'. Button with tooltip is in the top right corner. 'flipBehavior' is set to
   * ['top', 'right', 'left']. Since there is no space to the top, it checks if right is available. There's also no
   * space to the right, so it finally shows the tooltip on the left.
   */
  flipBehavior?:
    | 'flip'
    | (
        | 'top'
        | 'bottom'
        | 'left'
        | 'right'
        | 'top-start'
        | 'top-end'
        | 'bottom-start'
        | 'bottom-end'
        | 'left-start'
        | 'left-end'
        | 'right-start'
        | 'right-end'
      )[];
  /** Maximum width of the tooltip (default 18.75rem) */
  maxWidth?: string;
  /**
   * Tooltip position. Note: With 'enableFlip' set to true,
   * it will change the position if there is not enough space for the starting position.
   * The behavior of where it flips to can be controlled through the flipBehavior prop.
   * The 'auto' position chooses the side with the most space.
   * The 'auto' position requires the 'enableFlip' prop to be true.
   */
  position?:
    | TooltipPosition
    | 'auto'
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'top-start'
    | 'top-end'
    | 'bottom-start'
    | 'bottom-end'
    | 'left-start'
    | 'left-end'
    | 'right-start'
    | 'right-end';
  /**
   * Tooltip trigger: click, mouseenter, focus, manual
   * Set to manual to trigger tooltip programmatically (through the isVisible prop)
   */
  trigger?: string;
  /** Flag to indicate that the text content is left aligned */
  isContentLeftAligned?: boolean;
  /** value for visibility when trigger is 'manual' */
  isVisible?: boolean;
  /** z-index of the tooltip */
  zIndex?: number;
  /** id of the tooltip */
  id?: string;
  /** CSS fade transition animation duration */
  animationDuration?: number;
  /** @deprecated - no longer used. if you want to constrain the popper to a specific element use the appendTo prop instead */
  boundary?: 'scrollParent' | 'window' | 'viewport' | HTMLElement;
  /** @deprecated - no longer used */
  isAppLauncher?: boolean;
  /** @deprecated - no longer used */
  tippyProps?: Partial<TippyProps>;
}

// id for associating trigger with the content aria-describedby or aria-labelledby
let pfTooltipIdCounter = 1;

export const Tooltip: React.FunctionComponent<TooltipProps> = ({
  content: bodyContent,
  position = 'top',
  trigger = 'mouseenter focus',
  isVisible = false,
  isContentLeftAligned = false,
  enableFlip = true,
  className = '',
  entryDelay = 300,
  exitDelay = 300,
  appendTo = () => document.body,
  zIndex = 9999,
  maxWidth = tooltipMaxWidth.value,
  distance = 15,
  aria = 'describedby',
  // For every initial starting position, there are 3 escape positions
  flipBehavior = ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
  id = `pf-tooltip-${pfTooltipIdCounter++}`,
  children,
  animationDuration = 300,
  reference,
  'aria-live': ariaLive = reference ? 'polite' : 'off',
  boundary,
  isAppLauncher,
  tippyProps,
  ...rest
}: TooltipProps) => {
  if (process.env.NODE_ENV !== 'production') {
    boundary !== undefined &&
      console.warn(
        'The Tooltip boundary prop has been deprecated. If you want to constrain the popper to a specific element use the appendTo prop instead.'
      );
    isAppLauncher !== undefined &&
      console.warn('The Tooltip isAppLauncher prop has been deprecated and is no longer used.');
    tippyProps !== undefined && console.warn('The Tooltip tippyProps prop has been deprecated and is no longer used.');
  }
  // could make this a prop in the future (true | false | 'toggle')
  const hideOnClick = true;
  const triggerOnMouseenter = trigger.includes('mouseenter');
  const triggerOnFocus = trigger.includes('focus');
  const triggerOnClick = trigger.includes('click');
  const triggerManually = trigger === 'manual';
  const [visible, setVisible] = React.useState(false);
  const [opacity, setOpacity] = React.useState(0);
  const transitionTimerRef = React.useRef(null);
  const showTimerRef = React.useRef(null);
  const hideTimerRef = React.useRef(null);

  const clearTimeouts = (timeoutRefs: React.RefObject<any>[]) => {
    timeoutRefs.forEach(ref => {
      if (ref.current) {
        clearTimeout(ref.current);
      }
    });
  };

  // Cancel all timers on unmount
  React.useEffect(
    () => () => {
      clearTimeouts([transitionTimerRef, hideTimerRef, showTimerRef]);
    },
    []
  );

  const onDocumentKeyDown = (event: KeyboardEvent) => {
    if (!triggerManually) {
      if (event.keyCode === KEY_CODES.ESCAPE_KEY && visible) {
        hide();
      }
    }
  };
  const onTriggerEnter = (event: KeyboardEvent) => {
    if (event.keyCode === KEY_CODES.ENTER) {
      if (!visible) {
        show();
      } else {
        hide();
      }
    }
  };
  React.useEffect(() => {
    if (isVisible) {
      show();
    } else {
      hide();
    }
  }, [isVisible]);
  const show = () => {
    clearTimeouts([transitionTimerRef, hideTimerRef]);
    showTimerRef.current = setTimeout(() => {
      setVisible(true);
      setOpacity(1);
    }, entryDelay);
  };
  const hide = () => {
    clearTimeouts([showTimerRef]);
    hideTimerRef.current = setTimeout(() => {
      setOpacity(0);
      transitionTimerRef.current = setTimeout(() => setVisible(false), animationDuration);
    }, exitDelay);
  };
  const positionModifiers = {
    top: styles.modifiers.top,
    bottom: styles.modifiers.bottom,
    left: styles.modifiers.left,
    right: styles.modifiers.right,
    'top-start': styles.modifiers.topLeft,
    'top-end': styles.modifiers.topRight,
    'bottom-start': styles.modifiers.bottomLeft,
    'bottom-end': styles.modifiers.bottomRight,
    'left-start': styles.modifiers.leftTop,
    'left-end': styles.modifiers.leftBottom,
    'right-start': styles.modifiers.rightTop,
    'right-end': styles.modifiers.rightBottom
  };
  const hasCustomMaxWidth = maxWidth !== tooltipMaxWidth.value;
  const content = (
    <div
      aria-live={ariaLive}
      className={css(styles.tooltip, className)}
      role="tooltip"
      id={id}
      style={{
        maxWidth: hasCustomMaxWidth ? maxWidth : null,
        opacity,
        transition: getOpacityTransition(animationDuration)
      }}
      {...rest}
    >
      <TooltipArrow />
      <TooltipContent isLeftAligned={isContentLeftAligned}>{bodyContent}</TooltipContent>
    </div>
  );

  const onDocumentClick = (event: MouseEvent, triggerElement: HTMLElement) => {
    // event.currentTarget = document
    // event.target could be triggerElement or something else
    if (hideOnClick === true) {
      // hide on inside the toggle as well as on outside clicks
      if (visible) {
        hide();
      } else if (event.target === triggerElement) {
        show();
      }
    } else if (hideOnClick === 'toggle' && event.target === triggerElement) {
      // prevent outside clicks from hiding but allow it to still be toggled on toggle click
      if (visible) {
        hide();
      } else {
        show();
      }
    } else if (hideOnClick === false && !visible && event.target === triggerElement) {
      show();
    }
  };

  const addAriaToTrigger = () => {
    if (aria === 'describedby' && children && children.props && !children.props['aria-describedby']) {
      return React.cloneElement(children, { 'aria-describedby': id });
    } else if (aria === 'labelledby' && children.props && !children.props['aria-labelledby']) {
      return React.cloneElement(children, { 'aria-labelledby': id });
    }
    return children;
  };

  return (
    <Popper
      trigger={aria !== 'none' && visible ? addAriaToTrigger() : children}
      reference={reference}
      popper={content}
      popperMatchesTriggerWidth={false}
      appendTo={appendTo}
      isVisible={visible}
      positionModifiers={positionModifiers}
      distance={distance}
      placement={position}
      onMouseEnter={triggerOnMouseenter && show}
      onMouseLeave={triggerOnMouseenter && hide}
      onPopperMouseEnter={triggerOnMouseenter && show}
      onPopperMouseLeave={triggerOnMouseenter && hide}
      onFocus={triggerOnFocus && show}
      onBlur={triggerOnFocus && hide}
      onDocumentClick={triggerOnClick && onDocumentClick}
      onDocumentKeyDown={triggerManually ? null : onDocumentKeyDown}
      onTriggerEnter={triggerManually ? null : onTriggerEnter}
      enableFlip={enableFlip}
      zIndex={zIndex}
      flipBehavior={flipBehavior}
    />
  );
};
Tooltip.displayName = 'Tooltip';
