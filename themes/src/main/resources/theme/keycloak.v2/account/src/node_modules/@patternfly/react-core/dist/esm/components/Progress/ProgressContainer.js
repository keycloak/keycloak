import * as React from 'react';
import progressStyle from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';
import { Tooltip } from '../Tooltip';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import { ProgressBar } from './ProgressBar';
export var ProgressMeasureLocation;
(function (ProgressMeasureLocation) {
    ProgressMeasureLocation["outside"] = "outside";
    ProgressMeasureLocation["inside"] = "inside";
    ProgressMeasureLocation["top"] = "top";
    ProgressMeasureLocation["none"] = "none";
})(ProgressMeasureLocation || (ProgressMeasureLocation = {}));
export var ProgressVariant;
(function (ProgressVariant) {
    ProgressVariant["danger"] = "danger";
    ProgressVariant["success"] = "success";
    ProgressVariant["warning"] = "warning";
})(ProgressVariant || (ProgressVariant = {}));
const variantToIcon = {
    danger: TimesCircleIcon,
    success: CheckCircleIcon,
    warning: ExclamationTriangleIcon
};
export const ProgressContainer = ({ progressBarAriaProps, value, title = '', parentId, label = null, variant = null, measureLocation = ProgressMeasureLocation.top, isTitleTruncated = false, tooltipPosition }) => {
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
    const Title = (React.createElement("div", { className: css(progressStyle.progressDescription, isTitleTruncated && typeof title === 'string' && progressStyle.modifiers.truncate), id: `${parentId}-description`, "aria-hidden": "true", onMouseEnter: isTitleTruncated && typeof title === 'string' ? onMouseEnter : null }, title));
    return (React.createElement(React.Fragment, null,
        tooltip ? (React.createElement(Tooltip, { position: tooltipPosition, content: tooltip, isVisible: true }, Title)) : (Title),
        React.createElement("div", { className: css(progressStyle.progressStatus), "aria-hidden": "true" },
            (measureLocation === ProgressMeasureLocation.top || measureLocation === ProgressMeasureLocation.outside) && (React.createElement("span", { className: css(progressStyle.progressMeasure) }, label || `${value}%`)),
            variantToIcon.hasOwnProperty(variant) && (React.createElement("span", { className: css(progressStyle.progressStatusIcon) },
                React.createElement(StatusIcon, null)))),
        React.createElement(ProgressBar, { role: "progressbar", progressBarAriaProps: progressBarAriaProps, value: value }, measureLocation === ProgressMeasureLocation.inside && `${value}%`)));
};
ProgressContainer.displayName = 'ProgressContainer';
//# sourceMappingURL=ProgressContainer.js.map