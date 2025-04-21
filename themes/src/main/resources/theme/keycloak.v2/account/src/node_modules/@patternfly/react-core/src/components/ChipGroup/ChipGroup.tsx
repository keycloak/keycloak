import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ChipGroup/chip-group';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Chip } from '../Chip';
import { Tooltip, TooltipPosition } from '../Tooltip';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
import { fillTemplate } from '../../helpers';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { getOUIAProps, OUIAProps } from '../../helpers';

export interface ChipGroupProps extends React.HTMLProps<HTMLUListElement>, OUIAProps {
  /** Content rendered inside the chip group. Should be <Chip> elements. */
  children?: React.ReactNode;
  /** Additional classes added to the chip item */
  className?: string;
  /** Flag for having the chip group default to expanded */
  defaultIsOpen?: boolean;
  /** Customizable "Show Less" text string */
  expandedText?: string;
  /** Customizeable template string. Use variable "${remaining}" for the overflow chip count. */
  collapsedText?: string;
  /** Category name text for the chip group category.  If this prop is supplied the chip group with have a label and category styling applied */
  categoryName?: string;
  /** Aria label for chip group that does not have a category name */
  'aria-label'?: string;
  /** Set number of chips to show before overflow */
  numChips?: number;
  /** Flag if chip group can be closed*/
  isClosable?: boolean;
  /** Aria label for close button */
  closeBtnAriaLabel?: string;
  /** Function that is called when clicking on the chip group close button */
  onClick?: (event: React.MouseEvent) => void;
  /** Function that is called when clicking on the overflow (expand/collapse) chip button */
  onOverflowChipClick?: (event: React.MouseEvent) => void;
  /** Position of the tooltip which is displayed if the category name text is longer */
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

interface ChipGroupState {
  isOpen: boolean;
  isTooltipVisible: boolean;
}

export class ChipGroup extends React.Component<ChipGroupProps, ChipGroupState> {
  static displayName = 'ChipGroup';
  constructor(props: ChipGroupProps) {
    super(props);
    this.state = {
      isOpen: this.props.defaultIsOpen,
      isTooltipVisible: false
    };
  }
  private headingRef = React.createRef<HTMLSpanElement>();

  static defaultProps: ChipGroupProps = {
    expandedText: 'Show Less',
    collapsedText: '${remaining} more',
    categoryName: '',
    defaultIsOpen: false,
    numChips: 3,
    isClosable: false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (_e: React.MouseEvent) => undefined as any,
    onOverflowChipClick: (_e: React.MouseEvent) => undefined as any,
    closeBtnAriaLabel: 'Close chip group',
    tooltipPosition: 'top',
    'aria-label': 'Chip group category'
  };

  componentDidMount() {
    this.setState({
      isTooltipVisible: Boolean(
        this.headingRef.current && this.headingRef.current.offsetWidth < this.headingRef.current.scrollWidth
      )
    });
  }

  toggleCollapse = () => {
    this.setState(prevState => ({
      isOpen: !prevState.isOpen,
      isTooltipVisible: Boolean(
        this.headingRef.current && this.headingRef.current.offsetWidth < this.headingRef.current.scrollWidth
      )
    }));
  };

  renderLabel(id: string) {
    const { categoryName, tooltipPosition } = this.props;
    const { isTooltipVisible } = this.state;
    return isTooltipVisible ? (
      <Tooltip position={tooltipPosition} content={categoryName}>
        <span tabIndex={0} ref={this.headingRef} className={css(styles.chipGroupLabel)}>
          <span id={id}>{categoryName}</span>
        </span>
      </Tooltip>
    ) : (
      <span ref={this.headingRef} className={css(styles.chipGroupLabel)} id={id}>
        {categoryName}
      </span>
    );
  }

  render() {
    const {
      categoryName,
      children,
      className,
      isClosable,
      closeBtnAriaLabel,
      'aria-label': ariaLabel,
      onClick,
      onOverflowChipClick,
      numChips,
      expandedText,
      collapsedText,
      ouiaId,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      defaultIsOpen,
      tooltipPosition,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      ...rest
    } = this.props;
    const { isOpen } = this.state;
    const numChildren = React.Children.count(children);
    const collapsedTextResult = fillTemplate(collapsedText as string, {
      remaining: React.Children.count(children) - numChips
    });

    const renderChipGroup = (id: string) => {
      const chipArray = !isOpen
        ? React.Children.toArray(children).slice(0, numChips)
        : React.Children.toArray(children);

      return (
        <div
          className={css(styles.chipGroup, className, categoryName && styles.modifiers.category)}
          role="group"
          {...(categoryName && { 'aria-labelledby': id })}
          {...(!categoryName && { 'aria-label': ariaLabel })}
          {...getOUIAProps(ChipGroup.displayName, ouiaId)}
        >
          <div className={css(styles.chipGroupMain)}>
            {categoryName && this.renderLabel(id)}
            <ul
              className={css(styles.chipGroupList)}
              {...(categoryName && { 'aria-labelledby': id })}
              {...(!categoryName && { 'aria-label': ariaLabel })}
              role="list"
              {...rest}
            >
              {chipArray.map((child, i) => (
                <li className={css(styles.chipGroupListItem)} key={i}>
                  {child}
                </li>
              ))}
              {numChildren > numChips && (
                <li className={css(styles.chipGroupListItem)}>
                  <Chip
                    isOverflowChip
                    onClick={event => {
                      this.toggleCollapse();
                      onOverflowChipClick(event);
                    }}
                    component="button"
                  >
                    {isOpen ? expandedText : collapsedTextResult}
                  </Chip>
                </li>
              )}
            </ul>
          </div>
          {isClosable && (
            <div className={css(styles.chipGroupClose)}>
              <Button
                variant="plain"
                aria-label={closeBtnAriaLabel}
                onClick={onClick}
                id={`remove_group_${id}`}
                aria-labelledby={`remove_group_${id} ${id}`}
                ouiaId={ouiaId || closeBtnAriaLabel}
              >
                <TimesCircleIcon aria-hidden="true" />
              </Button>
            </div>
          )}
        </div>
      );
    };

    return numChildren === 0 ? null : <GenerateId>{randomId => renderChipGroup(this.props.id || randomId)}</GenerateId>;
  }
}
