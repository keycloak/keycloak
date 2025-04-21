import { __rest } from "tslib";
import * as React from 'react';
import { useState } from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Alert/alert';
import accessibleStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import { AlertIcon } from './AlertIcon';
import { capitalize, useOUIAProps } from '../../helpers';
import { AlertContext } from './AlertContext';
import maxLines from '@patternfly/react-tokens/dist/esm/c_alert__title_max_lines';
import { Tooltip } from '../Tooltip';
import { AlertToggleExpandButton } from './AlertToggleExpandButton';
export var AlertVariant;
(function (AlertVariant) {
    AlertVariant["success"] = "success";
    AlertVariant["danger"] = "danger";
    AlertVariant["warning"] = "warning";
    AlertVariant["info"] = "info";
    AlertVariant["default"] = "default";
})(AlertVariant || (AlertVariant = {}));
export const Alert = (_a) => {
    var { variant = AlertVariant.default, isInline = false, isPlain = false, isLiveRegion = false, variantLabel = `${capitalize(variant)} alert:`, 'aria-label': ariaLabel = `${capitalize(variant)} Alert`, actionClose, actionLinks, title, titleHeadingLevel: TitleHeadingLevel = 'h4', children = '', className = '', ouiaId, ouiaSafe = true, timeout = false, timeoutAnimation = 3000, onTimeout = () => { }, truncateTitle = 0, tooltipPosition, customIcon, isExpandable = false, toggleAriaLabel = `${capitalize(variant)} alert details`, onMouseEnter = () => { }, onMouseLeave = () => { } } = _a, props = __rest(_a, ["variant", "isInline", "isPlain", "isLiveRegion", "variantLabel", 'aria-label', "actionClose", "actionLinks", "title", "titleHeadingLevel", "children", "className", "ouiaId", "ouiaSafe", "timeout", "timeoutAnimation", "onTimeout", "truncateTitle", "tooltipPosition", "customIcon", "isExpandable", "toggleAriaLabel", "onMouseEnter", "onMouseLeave"]);
    const ouiaProps = useOUIAProps(Alert.displayName, ouiaId, ouiaSafe, variant);
    const getHeadingContent = (React.createElement(React.Fragment, null,
        React.createElement("span", { className: css(accessibleStyles.screenReader) }, variantLabel),
        title));
    const titleRef = React.useRef(null);
    const divRef = React.useRef();
    const [isTooltipVisible, setIsTooltipVisible] = useState(false);
    React.useEffect(() => {
        if (!titleRef.current || !truncateTitle) {
            return;
        }
        titleRef.current.style.setProperty(maxLines.name, truncateTitle.toString());
        const showTooltip = titleRef.current && titleRef.current.offsetHeight < titleRef.current.scrollHeight;
        if (isTooltipVisible !== showTooltip) {
            setIsTooltipVisible(showTooltip);
        }
    }, [titleRef, truncateTitle, isTooltipVisible]);
    const [timedOut, setTimedOut] = useState(false);
    const [timedOutAnimation, setTimedOutAnimation] = useState(true);
    const [isMouseOver, setIsMouseOver] = useState();
    const [containsFocus, setContainsFocus] = useState();
    const dismissed = timedOut && timedOutAnimation && !isMouseOver && !containsFocus;
    React.useEffect(() => {
        timeout = timeout === true ? 8000 : Number(timeout);
        if (timeout > 0) {
            const timer = setTimeout(() => setTimedOut(true), timeout);
            return () => clearTimeout(timer);
        }
    }, []);
    React.useEffect(() => {
        const onDocumentFocus = () => {
            if (divRef.current) {
                if (divRef.current.contains(document.activeElement)) {
                    setContainsFocus(true);
                    setTimedOutAnimation(false);
                }
                else if (containsFocus) {
                    setContainsFocus(false);
                }
            }
        };
        document.addEventListener('focus', onDocumentFocus, true);
        return () => document.removeEventListener('focus', onDocumentFocus, true);
    }, [containsFocus]);
    React.useEffect(() => {
        if (containsFocus === false || isMouseOver === false) {
            const timer = setTimeout(() => setTimedOutAnimation(true), timeoutAnimation);
            return () => clearTimeout(timer);
        }
    }, [containsFocus, isMouseOver]);
    React.useEffect(() => {
        dismissed && onTimeout();
    }, [dismissed]);
    const [isExpanded, setIsExpanded] = useState(false);
    const onToggleExpand = () => {
        setIsExpanded(!isExpanded);
    };
    const myOnMouseEnter = (ev) => {
        setIsMouseOver(true);
        setTimedOutAnimation(false);
        onMouseEnter(ev);
    };
    const myOnMouseLeave = (ev) => {
        setIsMouseOver(false);
        onMouseLeave(ev);
    };
    if (dismissed) {
        return null;
    }
    const Title = (React.createElement(TitleHeadingLevel, Object.assign({}, (isTooltipVisible && { tabIndex: 0 }), { ref: titleRef, className: css(styles.alertTitle, truncateTitle && styles.modifiers.truncate) }), getHeadingContent));
    return (React.createElement("div", Object.assign({ ref: divRef, className: css(styles.alert, isInline && styles.modifiers.inline, isPlain && styles.modifiers.plain, isExpandable && styles.modifiers.expandable, isExpanded && styles.modifiers.expanded, styles.modifiers[variant], className), "aria-label": ariaLabel }, ouiaProps, (isLiveRegion && {
        'aria-live': 'polite',
        'aria-atomic': 'false'
    }), { onMouseEnter: myOnMouseEnter, onMouseLeave: myOnMouseLeave }, props),
        isExpandable && (React.createElement(AlertContext.Provider, { value: { title, variantLabel } },
            React.createElement("div", { className: css(styles.alertToggle) },
                React.createElement(AlertToggleExpandButton, { isExpanded: isExpanded, onToggleExpand: onToggleExpand, "aria-label": toggleAriaLabel })))),
        React.createElement(AlertIcon, { variant: variant, customIcon: customIcon }),
        isTooltipVisible ? (React.createElement(Tooltip, { content: getHeadingContent, position: tooltipPosition }, Title)) : (Title),
        actionClose && (React.createElement(AlertContext.Provider, { value: { title, variantLabel } },
            React.createElement("div", { className: css(styles.alertAction) }, actionClose))),
        children && (!isExpandable || (isExpandable && isExpanded)) && (React.createElement("div", { className: css(styles.alertDescription) }, children)),
        actionLinks && React.createElement("div", { className: css(styles.alertActionGroup) }, actionLinks)));
};
Alert.displayName = 'Alert';
//# sourceMappingURL=Alert.js.map