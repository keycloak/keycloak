"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Alert = exports.AlertVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_1 = require("react");
const react_styles_1 = require("@patternfly/react-styles");
const alert_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Alert/alert"));
const accessibility_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));
const AlertIcon_1 = require("./AlertIcon");
const helpers_1 = require("../../helpers");
const AlertContext_1 = require("./AlertContext");
const c_alert__title_max_lines_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_alert__title_max_lines'));
const Tooltip_1 = require("../Tooltip");
const AlertToggleExpandButton_1 = require("./AlertToggleExpandButton");
var AlertVariant;
(function (AlertVariant) {
    AlertVariant["success"] = "success";
    AlertVariant["danger"] = "danger";
    AlertVariant["warning"] = "warning";
    AlertVariant["info"] = "info";
    AlertVariant["default"] = "default";
})(AlertVariant = exports.AlertVariant || (exports.AlertVariant = {}));
const Alert = (_a) => {
    var { variant = AlertVariant.default, isInline = false, isPlain = false, isLiveRegion = false, variantLabel = `${helpers_1.capitalize(variant)} alert:`, 'aria-label': ariaLabel = `${helpers_1.capitalize(variant)} Alert`, actionClose, actionLinks, title, titleHeadingLevel: TitleHeadingLevel = 'h4', children = '', className = '', ouiaId, ouiaSafe = true, timeout = false, timeoutAnimation = 3000, onTimeout = () => { }, truncateTitle = 0, tooltipPosition, customIcon, isExpandable = false, toggleAriaLabel = `${helpers_1.capitalize(variant)} alert details`, onMouseEnter = () => { }, onMouseLeave = () => { } } = _a, props = tslib_1.__rest(_a, ["variant", "isInline", "isPlain", "isLiveRegion", "variantLabel", 'aria-label', "actionClose", "actionLinks", "title", "titleHeadingLevel", "children", "className", "ouiaId", "ouiaSafe", "timeout", "timeoutAnimation", "onTimeout", "truncateTitle", "tooltipPosition", "customIcon", "isExpandable", "toggleAriaLabel", "onMouseEnter", "onMouseLeave"]);
    const ouiaProps = helpers_1.useOUIAProps(exports.Alert.displayName, ouiaId, ouiaSafe, variant);
    const getHeadingContent = (React.createElement(React.Fragment, null,
        React.createElement("span", { className: react_styles_1.css(accessibility_1.default.screenReader) }, variantLabel),
        title));
    const titleRef = React.useRef(null);
    const divRef = React.useRef();
    const [isTooltipVisible, setIsTooltipVisible] = react_1.useState(false);
    React.useEffect(() => {
        if (!titleRef.current || !truncateTitle) {
            return;
        }
        titleRef.current.style.setProperty(c_alert__title_max_lines_1.default.name, truncateTitle.toString());
        const showTooltip = titleRef.current && titleRef.current.offsetHeight < titleRef.current.scrollHeight;
        if (isTooltipVisible !== showTooltip) {
            setIsTooltipVisible(showTooltip);
        }
    }, [titleRef, truncateTitle, isTooltipVisible]);
    const [timedOut, setTimedOut] = react_1.useState(false);
    const [timedOutAnimation, setTimedOutAnimation] = react_1.useState(true);
    const [isMouseOver, setIsMouseOver] = react_1.useState();
    const [containsFocus, setContainsFocus] = react_1.useState();
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
    const [isExpanded, setIsExpanded] = react_1.useState(false);
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
    const Title = (React.createElement(TitleHeadingLevel, Object.assign({}, (isTooltipVisible && { tabIndex: 0 }), { ref: titleRef, className: react_styles_1.css(alert_1.default.alertTitle, truncateTitle && alert_1.default.modifiers.truncate) }), getHeadingContent));
    return (React.createElement("div", Object.assign({ ref: divRef, className: react_styles_1.css(alert_1.default.alert, isInline && alert_1.default.modifiers.inline, isPlain && alert_1.default.modifiers.plain, isExpandable && alert_1.default.modifiers.expandable, isExpanded && alert_1.default.modifiers.expanded, alert_1.default.modifiers[variant], className), "aria-label": ariaLabel }, ouiaProps, (isLiveRegion && {
        'aria-live': 'polite',
        'aria-atomic': 'false'
    }), { onMouseEnter: myOnMouseEnter, onMouseLeave: myOnMouseLeave }, props),
        isExpandable && (React.createElement(AlertContext_1.AlertContext.Provider, { value: { title, variantLabel } },
            React.createElement("div", { className: react_styles_1.css(alert_1.default.alertToggle) },
                React.createElement(AlertToggleExpandButton_1.AlertToggleExpandButton, { isExpanded: isExpanded, onToggleExpand: onToggleExpand, "aria-label": toggleAriaLabel })))),
        React.createElement(AlertIcon_1.AlertIcon, { variant: variant, customIcon: customIcon }),
        isTooltipVisible ? (React.createElement(Tooltip_1.Tooltip, { content: getHeadingContent, position: tooltipPosition }, Title)) : (Title),
        actionClose && (React.createElement(AlertContext_1.AlertContext.Provider, { value: { title, variantLabel } },
            React.createElement("div", { className: react_styles_1.css(alert_1.default.alertAction) }, actionClose))),
        children && (!isExpandable || (isExpandable && isExpanded)) && (React.createElement("div", { className: react_styles_1.css(alert_1.default.alertDescription) }, children)),
        actionLinks && React.createElement("div", { className: react_styles_1.css(alert_1.default.alertActionGroup) }, actionLinks)));
};
exports.Alert = Alert;
exports.Alert.displayName = 'Alert';
//# sourceMappingURL=Alert.js.map