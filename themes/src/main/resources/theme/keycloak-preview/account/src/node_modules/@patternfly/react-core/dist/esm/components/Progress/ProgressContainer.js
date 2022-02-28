import _pt from "prop-types";
import * as React from 'react';
import progressStyle from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';
import CheckCircleIcon from '@patternfly/react-icons/dist/js/icons/check-circle-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/js/icons/times-circle-icon';
import { ProgressBar } from './ProgressBar';
export let ProgressMeasureLocation;

(function (ProgressMeasureLocation) {
  ProgressMeasureLocation["outside"] = "outside";
  ProgressMeasureLocation["inside"] = "inside";
  ProgressMeasureLocation["top"] = "top";
  ProgressMeasureLocation["none"] = "none";
})(ProgressMeasureLocation || (ProgressMeasureLocation = {}));

export let ProgressVariant;

(function (ProgressVariant) {
  ProgressVariant["danger"] = "danger";
  ProgressVariant["success"] = "success";
  ProgressVariant["info"] = "info";
})(ProgressVariant || (ProgressVariant = {}));

const variantToIcon = {
  danger: TimesCircleIcon,
  success: CheckCircleIcon
};
export const ProgressContainer = ({
  ariaProps,
  value,
  title = '',
  parentId,
  label = null,
  variant = ProgressVariant.info,
  measureLocation = ProgressMeasureLocation.top
}) => {
  const StatusIcon = variantToIcon.hasOwnProperty(variant) && variantToIcon[variant];
  return React.createElement(React.Fragment, null, React.createElement("div", {
    className: css(progressStyle.progressDescription),
    id: `${parentId}-description`
  }, title), React.createElement("div", {
    className: css(progressStyle.progressStatus)
  }, (measureLocation === ProgressMeasureLocation.top || measureLocation === ProgressMeasureLocation.outside) && React.createElement("span", {
    className: css(progressStyle.progressMeasure)
  }, label || `${value}%`), variantToIcon.hasOwnProperty(variant) && React.createElement("span", {
    className: css(progressStyle.progressStatusIcon)
  }, React.createElement(StatusIcon, null))), React.createElement(ProgressBar, {
    ariaProps: ariaProps,
    value: value
  }, measureLocation === ProgressMeasureLocation.inside && `${value}%`));
};
ProgressContainer.propTypes = {
  ariaProps: _pt.any,
  parentId: _pt.string.isRequired,
  title: _pt.string,
  label: _pt.node,
  variant: _pt.oneOf(['danger', 'success', 'info']),
  measureLocation: _pt.oneOf(['outside', 'inside', 'top', 'none']),
  value: _pt.number.isRequired
};
//# sourceMappingURL=ProgressContainer.js.map