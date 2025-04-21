"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProgressContainer = exports.ProgressVariant = exports.ProgressMeasureLocation = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const progress_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Progress/progress"));
const react_styles_1 = require("@patternfly/react-styles");
const Tooltip_1 = require("../Tooltip");
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const times_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-circle-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const ProgressBar_1 = require("./ProgressBar");
var ProgressMeasureLocation;
(function (ProgressMeasureLocation) {
    ProgressMeasureLocation["outside"] = "outside";
    ProgressMeasureLocation["inside"] = "inside";
    ProgressMeasureLocation["top"] = "top";
    ProgressMeasureLocation["none"] = "none";
})(ProgressMeasureLocation = exports.ProgressMeasureLocation || (exports.ProgressMeasureLocation = {}));
var ProgressVariant;
(function (ProgressVariant) {
    ProgressVariant["danger"] = "danger";
    ProgressVariant["success"] = "success";
    ProgressVariant["warning"] = "warning";
})(ProgressVariant = exports.ProgressVariant || (exports.ProgressVariant = {}));
const variantToIcon = {
    danger: times_circle_icon_1.default,
    success: check_circle_icon_1.default,
    warning: exclamation_triangle_icon_1.default
};
const ProgressContainer = ({ progressBarAriaProps, value, title = '', parentId, label = null, variant = null, measureLocation = ProgressMeasureLocation.top, isTitleTruncated = false, tooltipPosition }) => {
    const StatusIcon = variantToIcon.hasOwnProperty(variant) && variantToIcon[variant];
    const [tooltip, setTooltip] = React.useState('');
    const onMouseEnter = (event) => {
        if (event.target.offsetWidth < event.target.scrollWidth) {
            setTooltip(title || event.target.innerHTML);
        }
        else {
            setTooltip('');
        }
    };
    const Title = (React.createElement("div", { className: react_styles_1.css(progress_1.default.progressDescription, isTitleTruncated && typeof title === 'string' && progress_1.default.modifiers.truncate), id: `${parentId}-description`, "aria-hidden": "true", onMouseEnter: isTitleTruncated && typeof title === 'string' ? onMouseEnter : null }, title));
    return (React.createElement(React.Fragment, null,
        tooltip ? (React.createElement(Tooltip_1.Tooltip, { position: tooltipPosition, content: tooltip, isVisible: true }, Title)) : (Title),
        React.createElement("div", { className: react_styles_1.css(progress_1.default.progressStatus), "aria-hidden": "true" },
            (measureLocation === ProgressMeasureLocation.top || measureLocation === ProgressMeasureLocation.outside) && (React.createElement("span", { className: react_styles_1.css(progress_1.default.progressMeasure) }, label || `${value}%`)),
            variantToIcon.hasOwnProperty(variant) && (React.createElement("span", { className: react_styles_1.css(progress_1.default.progressStatusIcon) },
                React.createElement(StatusIcon, null)))),
        React.createElement(ProgressBar_1.ProgressBar, { role: "progressbar", progressBarAriaProps: progressBarAriaProps, value: value }, measureLocation === ProgressMeasureLocation.inside && `${value}%`)));
};
exports.ProgressContainer = ProgressContainer;
exports.ProgressContainer.displayName = 'ProgressContainer';
//# sourceMappingURL=ProgressContainer.js.map