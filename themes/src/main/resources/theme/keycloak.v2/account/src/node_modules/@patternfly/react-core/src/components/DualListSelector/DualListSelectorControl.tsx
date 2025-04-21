import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { Button, ButtonVariant } from '../Button';
import { Tooltip } from '../Tooltip';

export interface DualListSelectorControlProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onClick'> {
  /** Content to be rendered in the dual list selector control. */
  children?: React.ReactNode;
  /** @hide forwarded ref */
  innerRef?: React.Ref<any>;
  /** Flag indicating the control is disabled. */
  isDisabled?: boolean;
  /** Additional classes applied to the dual list selector control. */
  className?: string;
  /** Callback fired when dual list selector control is selected. */
  onClick?: (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
  /** Accessible label for the dual list selector control. */
  'aria-label'?: string;
  /** Content to be displayed in a tooltip on hover of control. */
  tooltipContent?: React.ReactNode;
  /** Additional tooltip properties passed to the tooltip. */
  tooltipProps?: any;
}

export const DualListSelectorControlBase: React.FunctionComponent<DualListSelectorControlProps> = ({
  innerRef,
  children = null,
  className,
  'aria-label': ariaLabel,
  isDisabled = true,
  onClick = () => {},
  tooltipContent,
  tooltipProps = {} as any,
  ...props
}: DualListSelectorControlProps) => {
  const ref = innerRef || React.useRef(null);
  return (
    <div className={css('pf-c-dual-list-selector__controls-item', className)} {...props}>
      <Button
        isDisabled={isDisabled}
        aria-disabled={isDisabled}
        variant={ButtonVariant.plain}
        onClick={onClick}
        aria-label={ariaLabel}
        tabIndex={-1}
        ref={ref}
      >
        {children}
      </Button>
      {tooltipContent && <Tooltip content={tooltipContent} position="left" reference={ref} {...tooltipProps} />}
    </div>
  );
};
DualListSelectorControlBase.displayName = 'DualListSelectorControlBase';

export const DualListSelectorControl = React.forwardRef((props: DualListSelectorControlProps, ref: React.Ref<any>) => (
  <DualListSelectorControlBase innerRef={ref} {...props} />
));

DualListSelectorControl.displayName = 'DualListSelectorControl';
