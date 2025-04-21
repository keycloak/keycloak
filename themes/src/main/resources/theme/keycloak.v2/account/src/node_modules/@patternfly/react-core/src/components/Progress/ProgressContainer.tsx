import * as React from 'react';
import progressStyle from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';
import { Tooltip, TooltipPosition } from '../Tooltip';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import { AriaProps, ProgressBar } from './ProgressBar';

export enum ProgressMeasureLocation {
  outside = 'outside',
  inside = 'inside',
  top = 'top',
  none = 'none'
}

export enum ProgressVariant {
  danger = 'danger',
  success = 'success',
  warning = 'warning'
}

export interface ProgressContainerProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label' | 'title'> {
  /** Properties needed for aria support */
  progressBarAriaProps?: AriaProps;
  /** Progress component DOM ID. */
  parentId: string;
  /** Progress title. The isTitleTruncated property will only affect string titles. Node title truncation must be handled manually. */
  title?: React.ReactNode;
  /** Label to indicate what progress is showing. */
  label?: React.ReactNode;
  /** Type of progress status. */
  variant?: 'danger' | 'success' | 'warning';
  /** Location of progress value. */
  measureLocation?: 'outside' | 'inside' | 'top' | 'none';
  /** Actual progress value. */
  value: number;
  /** Whether string title should be truncated */
  isTitleTruncated?: boolean;
  /** Position of the tooltip which is displayed if title is truncated */
  tooltipPosition?:
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
}

const variantToIcon = {
  danger: TimesCircleIcon,
  success: CheckCircleIcon,
  warning: ExclamationTriangleIcon
};

export const ProgressContainer: React.FunctionComponent<ProgressContainerProps> = ({
  progressBarAriaProps,
  value,
  title = '',
  parentId,
  label = null,
  variant = null,
  measureLocation = ProgressMeasureLocation.top,
  isTitleTruncated = false,
  tooltipPosition
}: ProgressContainerProps) => {
  const StatusIcon = variantToIcon.hasOwnProperty(variant) && variantToIcon[variant];
  const [tooltip, setTooltip] = React.useState('');
  const onMouseEnter = (event: any) => {
    if (event.target.offsetWidth < event.target.scrollWidth) {
      setTooltip(title || event.target.innerHTML);
    } else {
      setTooltip('');
    }
  };
  const Title = (
    <div
      className={css(
        progressStyle.progressDescription,
        isTitleTruncated && typeof title === 'string' && progressStyle.modifiers.truncate
      )}
      id={`${parentId}-description`}
      aria-hidden="true"
      onMouseEnter={isTitleTruncated && typeof title === 'string' ? onMouseEnter : null}
    >
      {title}
    </div>
  );

  return (
    <React.Fragment>
      {tooltip ? (
        <Tooltip position={tooltipPosition} content={tooltip} isVisible>
          {Title}
        </Tooltip>
      ) : (
        Title
      )}
      <div className={css(progressStyle.progressStatus)} aria-hidden="true">
        {(measureLocation === ProgressMeasureLocation.top || measureLocation === ProgressMeasureLocation.outside) && (
          <span className={css(progressStyle.progressMeasure)}>{label || `${value}%`}</span>
        )}
        {variantToIcon.hasOwnProperty(variant) && (
          <span className={css(progressStyle.progressStatusIcon)}>
            <StatusIcon />
          </span>
        )}
      </div>
      <ProgressBar role="progressbar" progressBarAriaProps={progressBarAriaProps} value={value}>
        {measureLocation === ProgressMeasureLocation.inside && `${value}%`}
      </ProgressBar>
    </React.Fragment>
  );
};
ProgressContainer.displayName = 'ProgressContainer';
