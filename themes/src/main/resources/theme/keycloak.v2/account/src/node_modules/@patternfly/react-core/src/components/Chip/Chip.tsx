import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Tooltip, TooltipPosition } from '../Tooltip';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import styles from '@patternfly/react-styles/css/components/Chip/chip';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { getOUIAProps, OUIAProps, getDefaultOUIAId } from '../../helpers';

export interface ChipProps extends React.HTMLProps<HTMLDivElement>, OUIAProps {
  /** Content rendered inside the chip text */
  children?: React.ReactNode;
  /** Aria Label for close button */
  closeBtnAriaLabel?: string;
  /** Additional classes added to the chip item */
  className?: string;
  /** Flag indicating if the chip is an overflow chip */
  isOverflowChip?: boolean;
  /** Flag indicating if chip is read only */
  isReadOnly?: boolean;
  /** Function that is called when clicking on the chip close button */
  onClick?: (event: React.MouseEvent) => void;
  /** Component that will be used for chip. It is recommended that <button> or <li>  are used when the chip is an overflow chip. */
  component?: React.ReactNode;
  /** Position of the tooltip which is displayed if text is longer */
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

  /** Css property expressed in percentage or any css unit that overrides the default value of the max-width of the chip's text */
  textMaxWidth?: string;
}

interface ChipState {
  isTooltipVisible: boolean;
  ouiaStateId: string;
}

export class Chip extends React.Component<ChipProps, ChipState> {
  static displayName = 'Chip';
  constructor(props: ChipProps) {
    super(props);
    this.state = {
      isTooltipVisible: false,
      ouiaStateId: getDefaultOUIAId(Chip.displayName)
    };
  }
  span = React.createRef<HTMLSpanElement>();

  static defaultProps: ChipProps = {
    closeBtnAriaLabel: 'close',
    className: '',
    isOverflowChip: false,
    isReadOnly: false,
    tooltipPosition: 'top' as 'auto' | 'top' | 'bottom' | 'left' | 'right',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (_e: React.MouseEvent) => undefined as any,
    component: 'div' as React.ReactNode
  };

  componentDidMount() {
    this.setState({
      isTooltipVisible: Boolean(this.span.current && this.span.current.offsetWidth < this.span.current.scrollWidth)
    });
  }

  setChipStyle = () => ({
    '--pf-c-chip__text--MaxWidth': this.props.textMaxWidth
  });

  renderOverflowChip = () => {
    const { children, className, onClick, ouiaId } = this.props;
    const Component = this.props.component as any;
    return (
      <Component
        onClick={onClick}
        {...(this.props.textMaxWidth && {
          style: this.setChipStyle(),
          ...this.props.style
        })}
        className={css(styles.chip, styles.modifiers.overflow, className)}
        {...(this.props.component === 'button' ? { type: 'button' } : {})}
        {...getOUIAProps('OverflowChip', ouiaId !== undefined ? ouiaId : this.state.ouiaStateId)}
      >
        <span className={css(styles.chipText)}>{children}</span>
      </Component>
    );
  };

  renderInnerChip(id: string) {
    const { children, className, onClick, closeBtnAriaLabel, isReadOnly, component, ouiaId } = this.props;
    const Component = component as any;
    return (
      <Component
        {...(this.props.textMaxWidth && {
          style: this.setChipStyle()
        })}
        className={css(styles.chip, className)}
        {...(this.state.isTooltipVisible && { tabIndex: 0 })}
        {...getOUIAProps(Chip.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId)}
      >
        <span ref={this.span} className={css(styles.chipText)} id={id}>
          {children}
        </span>
        {!isReadOnly && (
          <Button
            onClick={onClick}
            variant="plain"
            aria-label={closeBtnAriaLabel}
            id={`remove_${id}`}
            aria-labelledby={`remove_${id} ${id}`}
            ouiaId={ouiaId || closeBtnAriaLabel}
          >
            <TimesIcon aria-hidden="true" />
          </Button>
        )}
      </Component>
    );
  }

  renderChip = (randomId: string) => {
    const { children, tooltipPosition } = this.props;
    if (this.state.isTooltipVisible) {
      return (
        <Tooltip position={tooltipPosition} content={children}>
          {this.renderInnerChip(randomId)}
        </Tooltip>
      );
    }
    return this.renderInnerChip(randomId);
  };

  render() {
    const { isOverflowChip } = this.props;
    return (
      <GenerateId>
        {randomId => (isOverflowChip ? this.renderOverflowChip() : this.renderChip(this.props.id || randomId))}
      </GenerateId>
    );
  }
}
