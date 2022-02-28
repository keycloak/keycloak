---
title: 'Filterable table'
section: 'demos'
---

## Examples

Uses the DataToolbar component to filter results in the table based on user input.

import {
  Title,
  Bullseye,
  DataToolbar,
  DataToolbarItem,
  DataToolbarContent,
  DataToolbarFilter,
  DataToolbarGroup,
  DataToolbarToggleGroup,
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions,
  Select,
  SelectOption,
  SelectVariant
} from '@patternfly/react-core';
import { SearchIcon } from '@patternfly/react-icons';
import { Table, TableHeader, TableBody} from '@patternfly/react-table';

```js title=Basic
import React from 'react';
import {
  Button,
  ButtonVariant,
  Bullseye,
  DataToolbar,
  DataToolbarItem,
  DataToolbarContent,
  DataToolbarFilter,
  DataToolbarToggleGroup,
  DataToolbarGroup,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  DropdownToggle,
  InputGroup,
  Title,
  Select,
  SelectOption,
  SelectVariant,
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';
import { SearchIcon, FilterIcon } from '@patternfly/react-icons';
import { Table, TableHeader, TableBody, TextInput } from '@patternfly/react-table';

class FilterTableDemo extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      filters: {
        location: [],
        name: [],
        status: []
      },
      currentCategory: 'Status',
      isFilterDropdownOpen: false,
      isCategoryDropdownOpen: false,
      nameInput: '',
      columns: [
        { title: 'Servers' },
        { title: 'Threads' },
        { title: 'Applications' },
        { title: 'Workspaces' },
        { title: 'Status' },
        { title: 'Location' }
      ],
      rows: [
        { cells: ['US-Node 1', '5', '25', '5', 'Stopped', 'Raleigh'] },
        { cells: ['US-Node 2', '5', '30', '2', 'Down', 'Westford'] },
        { cells: ['US-Node 3', '13', '35', '12', 'Degraded', 'Boston'] },
        { cells: ['US-Node 4', '2', '5', '18', 'Needs Maintainence', 'Raleigh'] },
        { cells: ['US-Node 5', '7', '30', '5', 'Running', 'Boston'] },
        { cells: ['US-Node 6', '5', '20', '15', 'Stopped', 'Raleigh'] },
        { cells: ['CZ-Node 1', '12', '48', '13', 'Down', 'Brno'] },
        { cells: ['CZ-Node 2', '3', '8', '20', 'Running', 'Brno'] },
        { cells: ['CZ-Remote-Node 1', '15', '20', '10', 'Down', 'Brno'] },
        { cells: ['Bangalore-Node 1', '20', '30', '30', 'Running', 'Bangalore'] }
      ],
      inputValue: ''
    };

    this.onDelete = (type = '', id = '') => {
      if (type) {
        this.setState(prevState => {
          prevState.filters[type.toLowerCase()] = prevState.filters[type.toLowerCase()].filter(s => s !== id);
          return {
            filters: prevState.filters
          };
        });
      } else {
        this.setState({
          filters: {
            location: [],
            name: [],
            status: []
          }
        });
      }
    };

    this.onCategoryToggle = isOpen => {
      this.setState({
        isCategoryDropdownOpen: isOpen
      });
    };

    this.onCategorySelect = event => {
      this.setState({
        currentCategory: event.target.innerText,
        isCategoryDropdownOpen: !this.state.isCategoryDropdownOpen
      });
    };

    this.onFilterToggle = isOpen => {
      this.setState({
        isFilterDropdownOpen: isOpen
      });
    };

    this.onFilterSelect = event => {
      this.setState({
        isFilterDropdownOpen: !this.state.isFilterDropdownOpen
      });
    };

    this.onInputChange = newValue => {
      this.setState({ inputValue: newValue });
    };

    this.onRowSelect = (event, isSelected, rowId) => {
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
    };

    this.onStatusSelect = (event, selection) => {
      const checked = event.target.checked;
      this.setState(prevState => {
        const prevSelections = prevState.filters['status'];
        return {
          filters: {
            ...prevState.filters,
            status: checked ? [...prevSelections, selection] : prevSelections.filter(value => value !== selection)
          }
        };
      });
    };

    this.onNameInput = event => {
      if (event.key && event.key !== 'Enter') {
        return;
      }

      const { inputValue } = this.state;
      this.setState(prevState => {
        const prevFilters = prevState.filters['name'];
        return {
          filters: {
            ...prevState.filters,
            ['name']: prevFilters.includes(inputValue) ? prevFilters : [...prevFilters, inputValue]
          },
          inputValue: ''
        };
      });
    };

    this.onLocationSelect = (event, selection) => {
      this.setState(prevState => {
        return {
          filters: {
            ...prevState.filters,
            ['location']: [selection]
          }
        };
      });
      this.onFilterSelect();
    };
  }

  buildCategoryDropdown() {
    const { isCategoryDropdownOpen, currentCategory } = this.state;

    return (
      <DataToolbarItem>
        <Dropdown
          onSelect={this.onCategorySelect}
          position={DropdownPosition.left}
          toggle={
            <DropdownToggle onToggle={this.onCategoryToggle} style={{ width: '100%' }}>
              <FilterIcon /> {currentCategory}
            </DropdownToggle>
          }
          isOpen={isCategoryDropdownOpen}
          dropdownItems={[
            <DropdownItem key="cat1">Location</DropdownItem>,
            <DropdownItem key="cat2">Name</DropdownItem>,
            <DropdownItem key="cat3">Status</DropdownItem>
          ]}
          style={{ width: '100%' }}
        ></Dropdown>
      </DataToolbarItem>
    );
  }

  buildFilterDropdown() {
    const { currentCategory, isFilterDropdownOpen, inputValue, filters } = this.state;

    const locationMenuItems = [
      <SelectOption key="raleigh" value="Raleigh" />,
      <SelectOption key="westford" value="Westford" />,
      <SelectOption key="boston" value="Boston" />,
      <SelectOption key="brno" value="Brno" />,
      <SelectOption key="bangalore" value="Bangalore" />
    ];

    const statusMenuItems = [
      <SelectOption key="statusRunning" value="Running" />,
      <SelectOption key="statusStopped" value="Stopped" />,
      <SelectOption key="statusDown" value="Down" />,
      <SelectOption key="statusDegraded" value="Degraded" />,
      <SelectOption key="statusMaint" value="Needs Maintainence" />
    ];

    return (
      <React.Fragment>
        <DataToolbarFilter
          chips={filters.location}
          deleteChip={this.onDelete}
          categoryName="Location"
          showToolbarItem={currentCategory === 'Location'}
        >
          <Select
            aria-label="Location"
            onToggle={this.onFilterToggle}
            onSelect={this.onLocationSelect}
            selections={filters.location[0]}
            isExpanded={isFilterDropdownOpen}
            placeholderText="Any"
          >
            {locationMenuItems}
          </Select>
        </DataToolbarFilter>
        <DataToolbarFilter
          chips={filters.name}
          deleteChip={this.onDelete}
          categoryName="Name"
          showToolbarItem={currentCategory === 'Name'}
        >
          <InputGroup>
            <TextInput
              name="nameInput"
              id="nameInput1"
              type="search"
              aria-label="name filter"
              onChange={this.onInputChange}
              value={inputValue}
              placeholder="Filter by name..."
              onKeyDown={this.onNameInput}
            />
            <Button
              variant={ButtonVariant.control}
              aria-label="search button for search input"
              onClick={this.onNameInput}
            >
              <SearchIcon />
            </Button>
          </InputGroup>
        </DataToolbarFilter>
        <DataToolbarFilter
          chips={filters.status}
          deleteChip={this.onDelete}
          categoryName="Status"
          showToolbarItem={currentCategory === 'Status'}
        >
          <Select
            variant={SelectVariant.checkbox}
            aria-label="Status"
            onToggle={this.onFilterToggle}
            onSelect={this.onStatusSelect}
            selections={filters.status}
            isExpanded={isFilterDropdownOpen}
            placeholderText="Filter by status"
          >
            {statusMenuItems}
          </Select>
        </DataToolbarFilter>
      </React.Fragment>
    );
  }

  renderToolbar() {
    const { filters } = this.state;
    return (
      <DataToolbar
        id="data-toolbar-with-chip-groups"
        clearAllFilters={this.onDelete}
        collapseListedFiltersBreakpoint="xl"
      >
        <DataToolbarContent>
          <DataToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
            <DataToolbarGroup variant="filter-group">
              {this.buildCategoryDropdown()}
              {this.buildFilterDropdown()}
            </DataToolbarGroup>
          </DataToolbarToggleGroup>
        </DataToolbarContent>
      </DataToolbar>
    );
  }

  render() {
    const { loading, rows, columns, filters } = this.state;

    const filteredRows =
      filters.name.length > 0 || filters.location.length > 0 || filters.status.length > 0
        ? rows.filter(row => {
            return (
              (filters.name.length === 0 ||
                filters.name.some(name => row.cells[0].toLowerCase().includes(name.toLowerCase()))) &&
              (filters.location.length === 0 || filters.location.includes(row.cells[5])) &&
              (filters.status.length === 0 || filters.status.includes(row.cells[4]))
            );
          })
        : rows;

    return (
      <React.Fragment>
        {this.renderToolbar()}
        {!loading && filteredRows.length > 0 && (
          <Table cells={columns} rows={filteredRows} onSelect={this.onRowSelect} aria-label="Filterable Table Demo">
            <TableHeader />
            <TableBody />
          </Table>
        )}
        {!loading && filteredRows.length === 0 && (
          <React.Fragment>
            <Table cells={columns} rows={filteredRows} onSelect={this.onRowSelect} aria-label="Filterable Table Demo">
              <TableHeader />
              <TableBody />
            </Table>
            <Bullseye>
              <EmptyState>
                <EmptyStateIcon icon={SearchIcon} />
                <Title headingLevel="h5" size="lg">
                  No results found
                </Title>
                <EmptyStateBody>
                  No results match this filter criteria. Remove all filters or clear all filters to show results.
                </EmptyStateBody>
                <EmptyStateSecondaryActions>
                  <Button variant="link" onClick={() => this.onDelete(null)}>
                    Clear all filters
                  </Button>
                </EmptyStateSecondaryActions>
              </EmptyState>
            </Bullseye>
          </React.Fragment>
        )}
        {loading && (
          <center>
            <Title size="3xl">Please wait while loading data</Title>
          </center>
        )}
      </React.Fragment>
    );
  }
}
```
