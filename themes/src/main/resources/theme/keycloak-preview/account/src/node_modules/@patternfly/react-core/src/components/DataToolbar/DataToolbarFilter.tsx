import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { DataToolbarItem, DataToolbarItemProps } from './DataToolbarItem';
import { ChipGroup, Chip, ChipGroupToolbarItem } from '../../components/ChipGroup';
import { DataToolbarContentContext, DataToolbarContext } from './DataToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';

export interface DataToolbarChipGroup {
  /** A unique key to identify this chip group category */
  key: string;
  /** The category name to display for the chip group */
  name: string;
}

export interface DataToolbarChip {
  /** A unique key to identify this chip */
  key: string;
  /** The ReactNode to display in the chip */
  node: React.ReactNode;
}

export interface DataToolbarFilterProps extends DataToolbarItemProps {
  /** An array of strings to be displayed as chips in the expandable content */
  chips?: (string | DataToolbarChip)[];
  /** Callback passed by consumer used to delete a chip from the chips[] */
  deleteChip?: (category: string | DataToolbarChipGroup, chip: DataToolbarChip | string) => void;
  /** Content to be rendered inside the data toolbar item associated with the chip group */
  children: React.ReactNode;
  /** Unique category name to be used as a label for the chip group */
  categoryName: string | DataToolbarChipGroup;
  /** Flag to show the toolbar item */
  showToolbarItem?: boolean;
}

interface DataToolbarFilterState {
  isMounted: boolean;
}

export class DataToolbarFilter extends React.Component<DataToolbarFilterProps, DataToolbarFilterState> {
  static contextType: any = DataToolbarContext;
  static defaultProps: PickOptional<DataToolbarFilterProps> = {
    chips: [] as (string | DataToolbarChip)[],
    showToolbarItem: true
  };

  constructor(props: DataToolbarFilterProps) {
    super(props);
    this.state = {
      isMounted: false
    };
  }

  componentDidMount() {
    const { categoryName, chips } = this.props;
    this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
    this.setState({ isMounted: true });
  }

  componentDidUpdate() {
    const { categoryName, chips } = this.props;
    this.context.updateNumberFilters(typeof categoryName === 'string' ? categoryName : categoryName.name, chips.length);
  }

  render() {
    const { children, chips, deleteChip, categoryName, showToolbarItem, ...props } = this.props;
    const { isExpanded, chipGroupContentRef } = this.context;

    const chipGroup = chips.length ? (
      <DataToolbarItem variant="chip-group">
        <ChipGroup withToolbar>
          <ChipGroupToolbarItem
            key={typeof categoryName === 'string' ? categoryName : categoryName.key}
            categoryName={typeof categoryName === 'string' ? categoryName : categoryName.name}
          >
            {chips.map(chip =>
              typeof chip === 'string' ? (
                <Chip key={chip} onClick={() => deleteChip(categoryName, chip)}>
                  {chip}
                </Chip>
              ) : (
                <Chip key={chip.key} onClick={() => deleteChip(categoryName, chip)}>
                  {chip.node}
                </Chip>
              )
            )}
          </ChipGroupToolbarItem>
        </ChipGroup>
      </DataToolbarItem>
    ) : null;

    if (!isExpanded && this.state.isMounted) {
      return (
        <React.Fragment>
          {showToolbarItem && <DataToolbarItem {...props}>{children}</DataToolbarItem>}
          {ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild)}
        </React.Fragment>
      );
    }

    return (
      <DataToolbarContentContext.Consumer>
        {({ chipContainerRef }) => (
          <React.Fragment>
            {showToolbarItem && <DataToolbarItem {...props}>{children}</DataToolbarItem>}
            {chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current)}
          </React.Fragment>
        )}
      </DataToolbarContentContext.Consumer>
    );
  }
}
