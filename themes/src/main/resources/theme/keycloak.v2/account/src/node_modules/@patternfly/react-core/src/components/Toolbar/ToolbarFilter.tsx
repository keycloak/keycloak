import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { ToolbarItem, ToolbarItemProps } from './ToolbarItem';
import { ChipGroup } from '../ChipGroup';
import { Chip } from '../Chip';
import { ToolbarContentContext, ToolbarContext } from './ToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';

export interface ToolbarChipGroup {
  /** A unique key to identify this chip group category */
  key: string;
  /** The category name to display for the chip group */
  name: string;
}

export interface ToolbarChip {
  /** A unique key to identify this chip */
  key: string;
  /** The ReactNode to display in the chip */
  node: React.ReactNode;
}

export interface ToolbarFilterProps extends ToolbarItemProps {
  /** An array of strings to be displayed as chips in the expandable content */
  chips?: (string | ToolbarChip)[];
  /** Callback passed by consumer used to close the entire chip group */
  deleteChipGroup?: (category: string | ToolbarChipGroup) => void;
  /** Callback passed by consumer used to delete a chip from the chips[] */
  deleteChip?: (category: string | ToolbarChipGroup, chip: ToolbarChip | string) => void;
  /** Customizable "Show Less" text string for the chip group */
  chipGroupExpandedText?: string;
  /** Customizeable template string for the chip group. Use variable "${remaining}" for the overflow chip count. */
  chipGroupCollapsedText?: string;
  /** Content to be rendered inside the data toolbar item associated with the chip group */
  children: React.ReactNode;
  /** Unique category name to be used as a label for the chip group */
  categoryName: string | ToolbarChipGroup;
  /** Flag to show the toolbar item */
  showToolbarItem?: boolean;
}

interface ToolbarFilterState {
  isMounted: boolean;
}

export class ToolbarFilter extends React.Component<ToolbarFilterProps, ToolbarFilterState> {
  static displayName = 'ToolbarFilter';
  static contextType = ToolbarContext;
  context!: React.ContextType<typeof ToolbarContext>;
  static defaultProps: PickOptional<ToolbarFilterProps> = {
    chips: [] as (string | ToolbarChip)[],
    showToolbarItem: true
  };

  constructor(props: ToolbarFilterProps) {
    super(props);
    this.state = {
      isMounted: false
    };
  }

  componentDidMount() {
    const { categoryName, chips } = this.props;
    this.context.updateNumberFilters(
      typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
        ? categoryName.key
        : categoryName.toString(),
      chips.length
    );
    this.setState({ isMounted: true });
  }

  componentDidUpdate() {
    const { categoryName, chips } = this.props;
    this.context.updateNumberFilters(
      typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
        ? categoryName.key
        : categoryName.toString(),
      chips.length
    );
  }

  render() {
    const {
      children,
      chips,
      deleteChipGroup,
      deleteChip,
      chipGroupExpandedText,
      chipGroupCollapsedText,
      categoryName,
      showToolbarItem,
      ...props
    } = this.props;
    const { isExpanded, chipGroupContentRef } = this.context;
    const categoryKey =
      typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
        ? categoryName.key
        : categoryName.toString();

    const chipGroup = chips.length ? (
      <ToolbarItem variant="chip-group">
        <ChipGroup
          key={categoryKey}
          categoryName={typeof categoryName === 'string' ? categoryName : categoryName.name}
          isClosable={deleteChipGroup !== undefined}
          onClick={() => deleteChipGroup(categoryName)}
          collapsedText={chipGroupCollapsedText}
          expandedText={chipGroupExpandedText}
        >
          {chips.map(chip =>
            typeof chip === 'string' ? (
              <Chip key={chip} onClick={() => deleteChip(categoryKey, chip)}>
                {chip}
              </Chip>
            ) : (
              <Chip key={chip.key} onClick={() => deleteChip(categoryKey, chip)}>
                {chip.node}
              </Chip>
            )
          )}
        </ChipGroup>
      </ToolbarItem>
    ) : null;

    if (!isExpanded && this.state.isMounted) {
      return (
        <React.Fragment>
          {showToolbarItem && <ToolbarItem {...props}>{children}</ToolbarItem>}
          {ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild)}
        </React.Fragment>
      );
    }

    return (
      <ToolbarContentContext.Consumer>
        {({ chipContainerRef }) => (
          <React.Fragment>
            {showToolbarItem && <ToolbarItem {...props}>{children}</ToolbarItem>}
            {chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current)}
          </React.Fragment>
        )}
      </ToolbarContentContext.Consumer>
    );
  }
}
