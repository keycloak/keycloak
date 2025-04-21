import { __rest } from "tslib";
/* eslint-disable no-console */
import * as React from 'react';
import { KeyTypes } from '../../helpers/constants';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import { css } from '@patternfly/react-styles';
import { PopoverContext } from './PopoverContext';
import { PopoverContent } from './PopoverContent';
import { PopoverBody } from './PopoverBody';
import { PopoverHeader } from './PopoverHeader';
import { PopoverFooter } from './PopoverFooter';
import { PopoverCloseButton } from './PopoverCloseButton';
import { PopoverArrow } from './PopoverArrow';
import popoverMaxWidth from '@patternfly/react-tokens/dist/esm/c_popover_MaxWidth';
import popoverMinWidth from '@patternfly/react-tokens/dist/esm/c_popover_MinWidth';
import { FocusTrap } from '../../helpers';
import { Popper, getOpacityTransition } from '../../helpers/Popper/Popper';
import { getUniqueId } from '../../helpers/util';
export var PopoverPosition;
(function (PopoverPosition) {
    PopoverPosition["auto"] = "auto";
    PopoverPosition["top"] = "top";
    PopoverPosition["bottom"] = "bottom";
    PopoverPosition["left"] = "left";
    PopoverPosition["right"] = "right";
    PopoverPosition["topStart"] = "top-start";
    PopoverPosition["topEnd"] = "top-end";
    PopoverPosition["bottomStart"] = "bottom-start";
    PopoverPosition["bottomEnd"] = "bottom-end";
    PopoverPosition["leftStart"] = "left-start";
    PopoverPosition["leftEnd"] = "left-end";
    PopoverPosition["rightStart"] = "right-start";
    PopoverPosition["rightEnd"] = "right-end";
})(PopoverPosition || (PopoverPosition = {}));
const alertStyle = {
    default: styles.modifiers.default,
    info: styles.modifiers.info,
    success: styles.modifiers.success,
    warning: styles.modifiers.warning,
    danger: styles.modifiers.danger
};
export const Popover = (_a) => {
    var { children, position = 'top', enableFlip = true, className = '', isVisible = null, shouldClose = () => null, shouldOpen = () => null, 'aria-label': ariaLabel = '', bodyContent, headerContent = null, headerComponent = 'h6', headerIcon = null, alertSeverityVariant, alertSeverityScreenReaderText, footerContent = null, appendTo = () => document.body, hideOnOutsideClick = true, onHide = () => null, onHidden = () => null, onShow = () => null, onShown = () => null, onMount = () => null, zIndex = 9999, minWidth = popoverMinWidth && popoverMinWidth.value, maxWidth = popoverMaxWidth && popoverMaxWidth.value, closeBtnAriaLabel = 'Close', showClose = true, distance = 25, 
    // For every initial starting position, there are 3 escape positions
    flipBehavior = ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'], animationDuration = 300, id, withFocusTrap: propWithFocusTrap, boundary, tippyProps, reference, hasNoPadding = false, hasAutoWidth = false } = _a, rest = __rest(_a, ["children", "position", "enableFlip", "className", "isVisible", "shouldClose", "shouldOpen", 'aria-label', "bodyContent", "headerContent", "headerComponent", "headerIcon", "alertSeverityVariant", "alertSeverityScreenReaderText", "footerContent", "appendTo", "hideOnOutsideClick", "onHide", "onHidden", "onShow", "onShown", "onMount", "zIndex", "minWidth", "maxWidth", "closeBtnAriaLabel", "showClose", "distance", "flipBehavior", "animationDuration", "id", "withFocusTrap", "boundary", "tippyProps", "reference", "hasNoPadding", "hasAutoWidth"]);
    if (process.env.NODE_ENV !== 'production') {
        boundary !== undefined &&
            console.warn('The Popover boundary prop has been deprecated. If you want to constrain the popper to a specific element use the appendTo prop instead.');
        tippyProps !== undefined && console.warn('The Popover tippyProps prop has been deprecated and is no longer used.');
    }
    // could make this a prop in the future (true | false | 'toggle')
    // const hideOnClick = true;
    const uniqueId = id || getUniqueId();
    const triggerManually = isVisible !== null;
    const [visible, setVisible] = React.useState(false);
    const [opacity, setOpacity] = React.useState(0);
    const [focusTrapActive, setFocusTrapActive] = React.useState(Boolean(propWithFocusTrap));
    const transitionTimerRef = React.useRef(null);
    const showTimerRef = React.useRef(null);
    const hideTimerRef = React.useRef(null);
    React.useEffect(() => {
        onMount();
    }, []);
    React.useEffect(() => {
        if (triggerManually) {
            if (isVisible) {
                show();
            }
            else {
                hide();
            }
        }
    }, [isVisible, triggerManually]);
    const show = (withFocusTrap) => {
        onShow();
        if (transitionTimerRef.current) {
            clearTimeout(transitionTimerRef.current);
        }
        if (hideTimerRef.current) {
            clearTimeout(hideTimerRef.current);
        }
        showTimerRef.current = setTimeout(() => {
            setVisible(true);
            setOpacity(1);
            propWithFocusTrap !== false && withFocusTrap && setFocusTrapActive(true);
            onShown();
        }, 0);
    };
    const hide = () => {
        onHide();
        if (showTimerRef.current) {
            clearTimeout(showTimerRef.current);
        }
        hideTimerRef.current = setTimeout(() => {
            setVisible(false);
            setOpacity(0);
            setFocusTrapActive(false);
            transitionTimerRef.current = setTimeout(() => {
                onHidden();
            }, animationDuration);
        }, 0);
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
    const hasCustomMinWidth = minWidth !== popoverMinWidth.value;
    const hasCustomMaxWidth = maxWidth !== popoverMaxWidth.value;
    const onDocumentKeyDown = (event) => {
        if (event.key === KeyTypes.Escape && visible) {
            if (triggerManually) {
                shouldClose(null, hide, event);
            }
            else {
                hide();
            }
        }
    };
    const onDocumentClick = (event, triggerElement, popperElement) => {
        if (hideOnOutsideClick && visible) {
            // check if we clicked within the popper, if so don't do anything
            const isChild = popperElement && popperElement.contains(event.target);
            if (isChild) {
                // clicked within the popper
                return;
            }
            if (triggerManually) {
                shouldClose(null, hide, event);
            }
            else {
                hide();
            }
        }
    };
    const onTriggerClick = (event) => {
        if (triggerManually) {
            if (visible) {
                shouldClose(null, hide, event);
            }
            else {
                shouldOpen(show, event);
            }
        }
        else {
            if (visible) {
                hide();
            }
            else {
                show(true);
            }
        }
    };
    const onContentMouseDown = () => {
        if (focusTrapActive) {
            setFocusTrapActive(false);
        }
    };
    const closePopover = (event) => {
        event.stopPropagation();
        if (triggerManually) {
            shouldClose(null, hide, event);
        }
        else {
            hide();
        }
    };
    const content = (React.createElement(FocusTrap, Object.assign({ active: focusTrapActive, focusTrapOptions: {
            returnFocusOnDeactivate: true,
            clickOutsideDeactivates: true,
            tabbableOptions: { displayCheck: 'none' },
            fallbackFocus: () => {
                // If the popover's trigger is focused but scrolled out of view,
                // FocusTrap will throw an error when the Enter button is used on the trigger.
                // That is because the Popover is hidden when its trigger is out of view.
                // Provide a fallback in that case.
                let node = null;
                if (document && document.activeElement) {
                    node = document.activeElement;
                }
                return node;
            }
        }, preventScrollOnDeactivate: true, className: css(styles.popover, alertSeverityVariant && alertStyle[alertSeverityVariant], hasNoPadding && styles.modifiers.noPadding, hasAutoWidth && styles.modifiers.widthAuto, className), role: "dialog", "aria-modal": "true", "aria-label": headerContent ? undefined : ariaLabel, "aria-labelledby": headerContent ? `popover-${uniqueId}-header` : undefined, "aria-describedby": `popover-${uniqueId}-body`, onMouseDown: onContentMouseDown, style: {
            minWidth: hasCustomMinWidth ? minWidth : null,
            maxWidth: hasCustomMaxWidth ? maxWidth : null,
            opacity,
            transition: getOpacityTransition(animationDuration)
        } }, rest),
        React.createElement(PopoverArrow, null),
        React.createElement(PopoverContent, null,
            showClose && React.createElement(PopoverCloseButton, { onClose: closePopover, "aria-label": closeBtnAriaLabel }),
            headerContent && (React.createElement(PopoverHeader, { id: `popover-${uniqueId}-header`, icon: headerIcon, alertSeverityVariant: alertSeverityVariant, alertSeverityScreenReaderText: alertSeverityScreenReaderText || `${alertSeverityVariant} alert:`, titleHeadingLevel: headerComponent }, typeof headerContent === 'function' ? headerContent(hide) : headerContent)),
            React.createElement(PopoverBody, { id: `popover-${uniqueId}-body` }, typeof bodyContent === 'function' ? bodyContent(hide) : bodyContent),
            footerContent && (React.createElement(PopoverFooter, { id: `popover-${uniqueId}-footer` }, typeof footerContent === 'function' ? footerContent(hide) : footerContent)))));
    return (React.createElement(PopoverContext.Provider, { value: { headerComponent } },
        React.createElement(Popper, { trigger: children, reference: reference, popper: content, popperMatchesTriggerWidth: false, appendTo: appendTo, isVisible: visible, positionModifiers: positionModifiers, distance: distance, placement: position, onTriggerClick: onTriggerClick, onDocumentClick: onDocumentClick, onDocumentKeyDown: onDocumentKeyDown, enableFlip: enableFlip, zIndex: zIndex, flipBehavior: flipBehavior })));
};
Popover.displayName = 'Popover';
//# sourceMappingURL=Popover.js.map