import { __rest } from "tslib";
/* eslint-disable no-console */
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tooltip/tooltip';
import { css } from '@patternfly/react-styles';
import { TooltipContent } from './TooltipContent';
import { TooltipArrow } from './TooltipArrow';
import { KEY_CODES } from '../../helpers/constants';
import tooltipMaxWidth from '@patternfly/react-tokens/dist/esm/c_tooltip_MaxWidth';
import { Popper, getOpacityTransition } from '../../helpers/Popper/Popper';
export var TooltipPosition;
(function (TooltipPosition) {
    TooltipPosition["auto"] = "auto";
    TooltipPosition["top"] = "top";
    TooltipPosition["bottom"] = "bottom";
    TooltipPosition["left"] = "left";
    TooltipPosition["right"] = "right";
    TooltipPosition["topStart"] = "top-start";
    TooltipPosition["topEnd"] = "top-end";
    TooltipPosition["bottomStart"] = "bottom-start";
    TooltipPosition["bottomEnd"] = "bottom-end";
    TooltipPosition["leftStart"] = "left-start";
    TooltipPosition["leftEnd"] = "left-end";
    TooltipPosition["rightStart"] = "right-start";
    TooltipPosition["rightEnd"] = "right-end";
})(TooltipPosition || (TooltipPosition = {}));
// id for associating trigger with the content aria-describedby or aria-labelledby
let pfTooltipIdCounter = 1;
export const Tooltip = (_a) => {
    var { content: bodyContent, position = 'top', trigger = 'mouseenter focus', isVisible = false, isContentLeftAligned = false, enableFlip = true, className = '', entryDelay = 300, exitDelay = 300, appendTo = () => document.body, zIndex = 9999, maxWidth = tooltipMaxWidth.value, distance = 15, aria = 'describedby', 
    // For every initial starting position, there are 3 escape positions
    flipBehavior = ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'], id = `pf-tooltip-${pfTooltipIdCounter++}`, children, animationDuration = 300, reference, 'aria-live': ariaLive = reference ? 'polite' : 'off', boundary, isAppLauncher, tippyProps } = _a, rest = __rest(_a, ["content", "position", "trigger", "isVisible", "isContentLeftAligned", "enableFlip", "className", "entryDelay", "exitDelay", "appendTo", "zIndex", "maxWidth", "distance", "aria", "flipBehavior", "id", "children", "animationDuration", "reference", 'aria-live', "boundary", "isAppLauncher", "tippyProps"]);
    if (process.env.NODE_ENV !== 'production') {
        boundary !== undefined &&
            console.warn('The Tooltip boundary prop has been deprecated. If you want to constrain the popper to a specific element use the appendTo prop instead.');
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
    const clearTimeouts = (timeoutRefs) => {
        timeoutRefs.forEach(ref => {
            if (ref.current) {
                clearTimeout(ref.current);
            }
        });
    };
    // Cancel all timers on unmount
    React.useEffect(() => () => {
        clearTimeouts([transitionTimerRef, hideTimerRef, showTimerRef]);
    }, []);
    const onDocumentKeyDown = (event) => {
        if (!triggerManually) {
            if (event.keyCode === KEY_CODES.ESCAPE_KEY && visible) {
                hide();
            }
        }
    };
    const onTriggerEnter = (event) => {
        if (event.keyCode === KEY_CODES.ENTER) {
            if (!visible) {
                show();
            }
            else {
                hide();
            }
        }
    };
    React.useEffect(() => {
        if (isVisible) {
            show();
        }
        else {
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
    const content = (React.createElement("div", Object.assign({ "aria-live": ariaLive, className: css(styles.tooltip, className), role: "tooltip", id: id, style: {
            maxWidth: hasCustomMaxWidth ? maxWidth : null,
            opacity,
            transition: getOpacityTransition(animationDuration)
        } }, rest),
        React.createElement(TooltipArrow, null),
        React.createElement(TooltipContent, { isLeftAligned: isContentLeftAligned }, bodyContent)));
    const onDocumentClick = (event, triggerElement) => {
        // event.currentTarget = document
        // event.target could be triggerElement or something else
        if (hideOnClick === true) {
            // hide on inside the toggle as well as on outside clicks
            if (visible) {
                hide();
            }
            else if (event.target === triggerElement) {
                show();
            }
        }
        else if (hideOnClick === 'toggle' && event.target === triggerElement) {
            // prevent outside clicks from hiding but allow it to still be toggled on toggle click
            if (visible) {
                hide();
            }
            else {
                show();
            }
        }
        else if (hideOnClick === false && !visible && event.target === triggerElement) {
            show();
        }
    };
    const addAriaToTrigger = () => {
        if (aria === 'describedby' && children && children.props && !children.props['aria-describedby']) {
            return React.cloneElement(children, { 'aria-describedby': id });
        }
        else if (aria === 'labelledby' && children.props && !children.props['aria-labelledby']) {
            return React.cloneElement(children, { 'aria-labelledby': id });
        }
        return children;
    };
    return (React.createElement(Popper, { trigger: aria !== 'none' && visible ? addAriaToTrigger() : children, reference: reference, popper: content, popperMatchesTriggerWidth: false, appendTo: appendTo, isVisible: visible, positionModifiers: positionModifiers, distance: distance, placement: position, onMouseEnter: triggerOnMouseenter && show, onMouseLeave: triggerOnMouseenter && hide, onPopperMouseEnter: triggerOnMouseenter && show, onPopperMouseLeave: triggerOnMouseenter && hide, onFocus: triggerOnFocus && show, onBlur: triggerOnFocus && hide, onDocumentClick: triggerOnClick && onDocumentClick, onDocumentKeyDown: triggerManually ? null : onDocumentKeyDown, onTriggerEnter: triggerManually ? null : onTriggerEnter, enableFlip: enableFlip, zIndex: zIndex, flipBehavior: flipBehavior }));
};
Tooltip.displayName = 'Tooltip';
//# sourceMappingURL=Tooltip.js.map