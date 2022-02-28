---
title: Table column management
section: 'demos'
---

## Examples

import {
  Button,
  Checkbox,
  DataList,
  DataListCheck,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  DataToolbar,
  DataToolbarContent,
  DataToolbarGroup,
  DataToolbarItem,
  Modal,
  OptionsMenu,
  OptionsMenuToggle,
  Pagination,
  PaginationVariant,
  Text,
  TextContent,
  Select,
  SelectVariant
} from '@patternfly/react-core';
import { Table, TableHeader, TableBody } from '@patternfly/react-table';
import {
  CodeIcon,
  CodeBranchIcon,
  CubeIcon,
  FilterIcon,
  SortAmountDownIcon
} from '@patternfly/react-icons';

```js title=Column-management-action
import React from 'react';
import {
  Button,
  Checkbox,
  DataList,
  DataListCheck,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  DataToolbar,
  DataToolbarContent,
  DataToolbarGroup,
  DataToolbarItem,
  Modal,
  OptionsMenu,
  OptionsMenuToggle,
  Pagination,
  PaginationVariant,
  Text,
  TextContent,
  Select,
  SelectVariant
} from '@patternfly/react-core';
import { Table, TableHeader, TableBody } from '@patternfly/react-table';
import {
  CodeIcon,
  CodeBranchIcon,
  CubeIcon,
  FilterIcon,
  SortAmountDownIcon
} from '@patternfly/react-icons';

class ColumnManagementAction extends React.Component {
  constructor(props) {
    super(props);
    this.actions = [
      {
        title: <a href="#">Link</a>
      },
      {
        title: 'Action'
      },
      {
        isSeparator: true
      },
      {
        title: <a href="#">Separated link</a>
      }
    ];
    this.defaultColumns = [
      'Repositories',
      'Branches',
      'Pull requests',
      'Workspaces',
      'Last commit',
      ''
    ];
    this.defaultRows = [
      {
        cells: [
          {
            title: (
              <React.Fragment>
                <div>Node 1</div>
                <a href="#">siemur/test-space</a>
                </React.Fragment>
            ),
            props: { column: 'Repositories' }
          },
          {
            title: (
              <React.Fragment>
                <CodeBranchIcon key="icon" /> 10
              </React.Fragment>
            ),
            props: { column: 'Branches' }
          },
          {
            title: (
              <React.Fragment><CodeIcon key="icon" /> 25</React.Fragment>
            ),
            props: { column: 'Pull requests' }
          },
          {
            title: (
              <React.Fragment><CubeIcon key="icon" /> 5</React.Fragment>
            ),
            props: { column: 'Workspaces' }
          },
          {
            title: '2 days ago',
            props: { column: 'Last commit' }
          },
          {
            title: (
              <React.Fragment><a href="#">Action link</a></React.Fragment>
            ),
            props: { column: '' }
          }
        ]
      },
      {
        cells: [
          {
            title: (
              <React.Fragment>
                <div>Node 2</div>
                <a href="#">siemur/test-space</a>
                </React.Fragment>
            ),
            props: { column: 'Repositories' }
          },
          {
            title: (
              <React.Fragment>
                <CodeBranchIcon key="icon" /> 8
              </React.Fragment>
            ),
            props: { column: 'Branches' }
          },
          {
            title: (
              <React.Fragment><CodeIcon key="icon" /> 30</React.Fragment>
            ),
            props: { column: 'Pull requests' }
          },
          {
            title: (
              <React.Fragment><CubeIcon key="icon" /> 2</React.Fragment>
            ),
            props: { column: 'Workspaces' }
          },
          {
            title: '2 days ago',
            props: { column: 'Last commit' }
          },
          {
            title: (
              <React.Fragment><a href="#">Action link</a></React.Fragment>
            ),
            props: { column: '' }
          }
        ]
      },
      {
        cells: [
          {
            title:(
              <React.Fragment>
                <div>Node 3</div>
                <a href="#">siemur/test-space</a>
                </React.Fragment>
              ),
              props: { column: 'Repositories' }
          },
          {
            title: (
              <React.Fragment>
                <CodeBranchIcon key="icon" /> 12
              </React.Fragment>
            ),
            props: { column: 'Branches' }
          },
          {
            title: (
              <React.Fragment><CodeIcon key="icon" /> 48</React.Fragment>
            ),
            props: { column: 'Pull requests' }
          },
          {
            title: (
              <React.Fragment><CubeIcon key="icon" /> 13</React.Fragment>
            ),
            props: { column: 'Workspaces' }
          },
          {
            title: '30 days ago',
            props: { column: 'Last commit' }
          },
          {
            title: (
              <React.Fragment><a href="#">Action link</a></React.Fragment>
            ),
            props: { column: '' }
          }
        ]
      },
      {
        cells: [
          { title: (
              <React.Fragment>
                <div>Node 4</div>
                <a href="#">siemur/test-space</a>
              </React.Fragment>
            ),
            props: { column: 'Repositories' }
          },
          {
            title: (
              <React.Fragment>
                <CodeBranchIcon key="icon" /> 3
              </React.Fragment>
            ),
            props: { column: 'Branches' }
          },
          {
            title: (
              <React.Fragment><CodeIcon key="icon" /> 8</React.Fragment>
            ),
            props: { column: 'Pull requests' }
          },
          {
            title: (
              <React.Fragment><CubeIcon key="icon" /> 20</React.Fragment>
            ),
            props: { column: 'Workspaces' }
          },
          {
            title: '8 days ago',
            props: { column: 'Last commit' }
          },
          {
            title: (
              <React.Fragment><a href="#">Action link</a></React.Fragment>
            ),
            props: { column: '' }
          }
        ]
      },
      {
        cells: [
          {
            title: (
              <React.Fragment>
                <div>Node 5</div>
                <a href="#">siemur/test-space</a>
              </React.Fragment>
            ),
            props: { column: 'Repositories' }
          },
          {
            title: (
              <React.Fragment>
                <CodeBranchIcon key="icon" /> 34
              </React.Fragment>
            ),
            props: { column: 'Branches' }
          },
          {
            title: (
              <React.Fragment><CodeIcon key="icon" /> 21</React.Fragment>
            ),
            props: { column: 'Pull requests' }
          },
          {
            title: (
              <React.Fragment><CubeIcon key="icon" /> 26</React.Fragment>
            ),
            props: { column: 'Workspaces' }
          },
          {
            title: '2 days ago',
            props: { column: 'Last commit' }
          },
          {
            title: (
              <React.Fragment><a href="#">Action link</a></React.Fragment>
            ),
            props: { column: '' }
          }
        ]
      },
    ];
    this.state = {
      filters: [],
      filteredColumns: [],
      filteredRows: [],
      columns: this.defaultColumns,
      rows: this.defaultRows,
      canSelectAll: true,
      isModalOpen: false,
      check1: true,
      check2: true,
      check3: true,
      check4: true,
      check5: true
    };
    this.onSelect = this.onSelect.bind(this);
    this.toggleSelect = this.toggleSelect.bind(this);
    this.renderModal = this.renderModal.bind(this);
    this.matchCheckboxNameToColumn = (name) => {
      switch (name) {
        case 'check1':
          return 'Repositories';
        case 'check2':
          return 'Branches';
        case 'check3':
          return 'Pull requests';
        case 'check4':
          return 'Workspaces';
        case 'check5':
          return 'Last commit';
      }
    };
    this.filterData = (checked, name) => {
      const { rows, columns, filters } = this.state;
      if (checked) {
        const updatedFilters = filters.filter(item => item !== name);
        const filteredColumns = this.defaultColumns.filter(column => !updatedFilters.includes(column));
        const filteredRows = this.defaultRows.map(({...row}) => { row.cells = row.cells.filter(cell=>(!updatedFilters.includes(cell.props.column))); return row;});
        this.setState({
          filters: updatedFilters,
          filteredColumns: filteredColumns,
          filteredRows: filteredRows
        });
      } else {
        let updatedFilters = filters;
        updatedFilters.push(name);
        const filteredColumns = columns.filter(column => !filters.includes(column));
        const filteredRows = rows.map(({...row}) => { row.cells = row.cells.filter(cell=>(!filters.includes(cell.props.column))); return row;});
        this.setState({
          filters: updatedFilters,
          filteredColumns: filteredColumns,
          filteredRows: filteredRows
        });
      }
    };
    this.unfilterAllData = () => {
      this.setState({
        filters: [],
        filteredColumns: this.defaultColumns,
        filteredRows: this.defaultRows
      });
    };
    this.handleChange = (checked, event) => {
      const target = event.target;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      this.filterData(checked, this.matchCheckboxNameToColumn(target.name));
      this.setState({
        [target.name]: value,
      });
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
    this.onSave = () => {
      this.setState(({ filteredColumns, filteredRows, isModalOpen }) => ({
        columns: filteredColumns,
        rows: filteredRows,
        isModalOpen: !isModalOpen
      }));
    }
    this.selectAllColumns = () => {
      this.unfilterAllData();
      this.setState(({
        check1: true,
        check2: true,
        check3: true,
        check4: true,
        check5: true,
      }));
    };
  }

  onSelect(event, isSelected, rowId) {
    let rows;
    if (rowId === -1) {
      rows = this.state.rows.map(oneRow => {
        oneRow.selected = isSelected;
        return oneRow;
      });
    } else {
      rows = [...this.state.rows];
      rows[rowId].selected = isSelected;
    }
    this.setState({
      rows
    });
  }

  toggleSelect(checked) {
    this.setState({
      canSelectAll: checked
    });
  }

  renderModal() {
    const { isModalOpen } = this.state;
    return <Modal
      title="Manage columns"
      isOpen={isModalOpen}
      isSmall
      description={
        <TextContent>
          <Text component={TextVariants.p}>
            Selected categories will be displayed in the table.
          </Text>
          <Button isInline onClick={this.selectAllColumns} variant="link">Select all</Button>
        </TextContent>
      }
      onClose={this.handleModalToggle}
      actions={[
        <Button key="save" variant="primary" onClick={this.onSave}>
          Save
        </Button>,
        <Button key="cancel" variant="secondary" onClick={this.handleModalToggle}>
          Cancel
        </Button>
      ]}
      isFooterLeftAligned
    >
      <DataList aria-label="Table column management" id="table-column-management" isCompact>
        <DataListItem aria-labelledby="table-column-management-item1">
          <DataListItemRow>
            <DataListCheck aria-labelledby="table-column-management-item1" isChecked={this.state.check1} name="check1" onChange={this.handleChange} />
            <DataListItemCells
              dataListCells={[
                <DataListCell id="table-column-management-item1" key="table-column-management-item1">
                  Repositories
                </DataListCell>
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="table-column-management-item2">
          <DataListItemRow>
            <DataListCheck aria-labelledby="table-column-management-item2" isChecked={this.state.check2} name="check2" onChange={this.handleChange} />
            <DataListItemCells
              dataListCells={[
                <DataListCell id="table-column-management-item2" key="table-column-management-item2">
                  Branches
                </DataListCell>
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="table-column-management-item3">
          <DataListItemRow>
            <DataListCheck aria-labelledby="table-column-management-item3" isChecked={this.state.check3} name="check3" onChange={this.handleChange} />
            <DataListItemCells
              dataListCells={[
                <DataListCell id="table-column-management-item3" key="table-column-management-item3">
                  Pull requests
                </DataListCell>
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="table-column-management-item4">
          <DataListItemRow>
            <DataListCheck aria-labelledby="table-column-management-item4" isChecked={this.state.check4} name="check4" onChange={this.handleChange} />
            <DataListItemCells
              dataListCells={[
                <DataListCell id="table-column-management-item4" key="table-column-management-item4">
                  Workspaces
                </DataListCell>
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="table-column-management-item5">
          <DataListItemRow>
            <DataListCheck aria-labelledby="table-column-management-item5" isChecked={this.state.check5} name="check5" onChange={this.handleChange} />
            <DataListItemCells
              dataListCells={[
                <DataListCell id="table-column-management-item5" key="table-column-management-item5">
                  Last commit
                </DataListCell>
              ]}
            />
          </DataListItemRow>
        </DataListItem>
      </DataList>
    </Modal>;
  }

  render() {
    const { canSelectAll, columns, rows } = this.state;

    const dataToolbarItems = <React.Fragment>
      <span id="page-layout-table-column-management-action-toolbar-top-select-checkbox-label" hidden>Choose one</span>
      <DataToolbarContent>
      <DataToolbarItem>
        <Select
          id="page-layout-table-column-management-action-toolbar-top-select-checkbox-toggle"
          variant={SelectVariant.single}
          aria-label="Select Input"
          aria-labelledby="page-layout-table-column-management-action-toolbar-top-select-checkbox-label page-layout-table-column-management-action-toolbar-top-select-checkbox-toggle"
          placeholderText={<><FilterIcon /> Name</>}
        />
      </DataToolbarItem>
      <DataToolbarItem>
        <OptionsMenu
          id="page-layout-table-column-management-action-toolbar-top-options-menu-toggle"
          isPlain
          menuItems={[]}
          toggle={
            <OptionsMenuToggle
              toggleTemplate={<SortAmountDownIcon aria-hidden="true"/>}
              aria-label="Sort by"
              hideCaret/>
          }
        />
      </DataToolbarItem>
      <DataToolbarGroup variant="button-group">
      <DataToolbarItem><Button variant="primary">Action</Button></DataToolbarItem>
      <DataToolbarItem><Button variant="link" onClick={this.handleModalToggle}>Manage columns</Button></DataToolbarItem>
      </DataToolbarGroup>
      </DataToolbarContent>
      <DataToolbarContent>
        <Pagination
          itemCount={37}
          widgetId="pagination-options-menu-bottom"
          page={1}
          variant={PaginationVariant.bottom}
        />
      </DataToolbarContent>
    </React.Fragment>;

    return <React.Fragment>
      <Table
        gridBreakPoint='grid-xl'
        header={<DataToolbar id="page-layout-table-column-management-action-toolbar-top">{dataToolbarItems}</DataToolbar>}
        aria-label="This is a table with checkboxes"
        id="page-layout-table-column-management-action-table"
        onSelect={this.onSelect}
        cells={columns}
        rows={rows}
        actions={this.actions}
        canSelectAll={canSelectAll}
      >
        <TableHeader />
        <TableBody />
      </Table>
      <DataToolbar id="footer">
        <DataToolbarContent>
          <Pagination
            id="page-layout-table-column-management-action-toolbar-bottom"
            itemCount={37}
            widgetId="pagination-options-menu-bottom"
            page={1}
            variant={PaginationVariant.bottom}
          />
        </DataToolbarContent>
      </DataToolbar>
      {this.renderModal()}
    </React.Fragment>;
  }
}
```
