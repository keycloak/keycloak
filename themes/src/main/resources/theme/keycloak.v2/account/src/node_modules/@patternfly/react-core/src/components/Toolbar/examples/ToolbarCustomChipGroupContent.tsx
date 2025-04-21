import React from 'react';
import {
  Toolbar,
  ToolbarItem,
  ToolbarContent,
  ToolbarFilter,
  ToolbarToggleGroup,
  ToolbarGroup,
  Button,
  Select,
  SelectOption,
  SelectVariant
} from '@patternfly/react-core';
import FilterIcon from '@patternfly/react-icons/dist/esm/icons/filter-icon';
import EditIcon from '@patternfly/react-icons/dist/esm/icons/edit-icon';
import CloneIcon from '@patternfly/react-icons/dist/esm/icons/clone-icon';
import SyncIcon from '@patternfly/react-icons/dist/esm/icons/sync-icon';

export const ToolbarCustomChipGroupContent: React.FunctionComponent = () => {
  const [statusIsExpanded, setStatusIsExpanded] = React.useState<boolean>(false);
  const [riskIsExpanded, setRiskIsExpanded] = React.useState<boolean>(false);
  const [filters, setFilters] = React.useState({
    risk: ['Low'],
    status: ['New', 'Pending']
  });

  const onDelete = (type: string, id: string) => {
    if (type === 'Risk') {
      setFilters({ risk: filters.risk.filter((fil: string) => fil !== id), status: filters.status });
    } else if (type === 'Status') {
      setFilters({ risk: filters.risk, status: filters.status.filter((fil: string) => fil !== id) });
    } else {
      setFilters({ risk: [], status: [] });
    }
  };

  const onDeleteGroup = (type: string) => {
    if (type === 'Risk') {
      setFilters({ risk: [], status: filters.status });
    } else if (type === 'Status') {
      setFilters({ risk: filters.risk, status: [] });
    }
  };

  const onSelect = (type: 'Risk' | 'Status', event: React.MouseEvent | React.ChangeEvent, selection: string) => {
    const checked = (event.target as any).checked;
    if (type === 'Risk') {
      setFilters({
        risk: checked ? [...filters.risk, selection] : filters.risk.filter((fil: string) => fil !== selection),
        status: filters.status
      });
    } else if (type === 'Status') {
      setFilters({
        risk: filters.risk,
        status: checked ? [...filters.status, selection] : filters.status.filter((fil: string) => fil !== selection)
      });
    }
  };

  const statusMenuItems = [
    <SelectOption key="statusNew" value="New" />,
    <SelectOption key="statusPending" value="Pending" />,
    <SelectOption key="statusRunning" value="Running" />,
    <SelectOption key="statusCancelled" value="Cancelled" />
  ];

  const riskMenuItems = [
    <SelectOption key="riskLow" value="Low" />,
    <SelectOption key="riskMedium" value="Medium" />,
    <SelectOption key="riskHigh" value="High" />
  ];

  const toggleGroupItems = (
    <React.Fragment>
      <ToolbarGroup variant="filter-group">
        <ToolbarFilter
          chips={filters.status}
          deleteChip={(category, chip) => onDelete(category as string, chip as string)}
          deleteChipGroup={category => onDeleteGroup(category as string)}
          categoryName="Status"
        >
          <Select
            variant={SelectVariant.checkbox}
            aria-label="Status"
            onToggle={(isExpanded: boolean) => setStatusIsExpanded(isExpanded)}
            onSelect={(event, selection) => onSelect('Status', event, selection as string)}
            selections={filters.status}
            isOpen={statusIsExpanded}
            placeholderText="Status"
          >
            {statusMenuItems}
          </Select>
        </ToolbarFilter>
        <ToolbarFilter
          chips={filters.risk}
          deleteChip={(category, chip) => onDelete(category as string, chip as string)}
          deleteChipGroup={category => onDeleteGroup(category as string)}
          categoryName="Risk"
        >
          <Select
            variant={SelectVariant.checkbox}
            aria-label="Risk"
            onToggle={(isExpanded: boolean) => setRiskIsExpanded(isExpanded)}
            onSelect={(event, selection) => onSelect('Risk', event, selection as string)}
            selections={filters.risk}
            isOpen={riskIsExpanded}
            placeholderText="Risk"
          >
            {riskMenuItems}
          </Select>
        </ToolbarFilter>
      </ToolbarGroup>
    </React.Fragment>
  );

  const toolbarItems = (
    <React.Fragment>
      <ToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
        {toggleGroupItems}
      </ToolbarToggleGroup>
      <ToolbarGroup variant="icon-button-group">
        <ToolbarItem>
          <Button variant="plain" aria-label="edit">
            <EditIcon />
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button variant="plain" aria-label="clone">
            <CloneIcon />
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button variant="plain" aria-label="sync">
            <SyncIcon />
          </Button>
        </ToolbarItem>
      </ToolbarGroup>
    </React.Fragment>
  );

  const customChipGroupContent = (
    <React.Fragment>
      <ToolbarItem>
        <Button variant="link" onClick={() => {}} isInline>
          Save filters
        </Button>
      </ToolbarItem>
      <ToolbarItem>
        <Button variant="link" onClick={() => onDelete('', '')} isInline>
          Clear all filters
        </Button>
      </ToolbarItem>
    </React.Fragment>
  );

  return (
    <Toolbar
      id="toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      customChipGroupContent={customChipGroupContent}
    >
      <ToolbarContent>{toolbarItems}</ToolbarContent>
    </Toolbar>
  );
};
