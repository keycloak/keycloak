import * as React from 'react';
import { mount } from 'enzyme';
import { DataToolbar } from '../DataToolbar';
import { DataToolbarContent } from '../DataToolbarContent';
import { DataToolbarGroup } from '../DataToolbarGroup';
import CloneIcon from '@patternfly/react-icons/dist/js/icons/clone-icon';
import EditIcon from '@patternfly/react-icons/dist/js/icons/edit-icon';
import FilterIcon from '@patternfly/react-icons/dist/js/icons/filter-icon';
import { Button } from '../../../components/Button';
import { DataToolbarItem } from '../DataToolbarItem';
import { DataToolbarFilter } from '../DataToolbarFilter';
import { DataToolbarToggleGroup } from '../DataToolbarToggleGroup';
import { Select, SelectOption, SelectVariant } from '../../../components/Select';

describe('data toolbar', () => {
  test('DataToolbarOneContent', () => {
    const view = mount(
      <DataToolbar id="data-toolbar" className="DataToolbar-class">
        <DataToolbarContent className="DataToolbarContent-class" />
      </DataToolbar>
    );
    expect(view).toMatchSnapshot();
  });

  test('DataToolbarTwoContent', () => {
    const view = mount(
      <DataToolbar id="data-toolbar" className="DataToolbar-class">
        <DataToolbarContent className="DataToolbarContent-class" />
        <DataToolbarContent className="DataToolbarContent-class" />
      </DataToolbar>
    );
    expect(view).toMatchSnapshot();
  });

  test('DataToolbarItemsAndGroups', () => {
    const view = mount(
      <DataToolbar id="data-toolbar" className="DataToolbar-class">
        <DataToolbarContent className="DataToolbarContent-class">
          <DataToolbarGroup variant="icon-button-group">
            <DataToolbarItem>
              <Button variant="plain">
                <EditIcon />
              </Button>
            </DataToolbarItem>
            <DataToolbarItem>
              <Button variant="plain">
                <CloneIcon />
              </Button>
            </DataToolbarItem>
          </DataToolbarGroup>
        </DataToolbarContent>
      </DataToolbar>
    );
    expect(view).toMatchSnapshot();
  });

  test('DataToolbarToggleGroup', () => {
    const statusOptions = [{ value: 'Running', disabled: false }, { value: 'Cancelled', disabled: false }];

    const riskOptions = [{ value: 'Low', disabled: false }, { value: 'High', disabled: false }];

    const onStatusToggle = () => {};
    const onRiskToggle = () => {};
    const onStatusSelect = () => {};
    const onRiskSelect = () => {};

    const view = mount(
      <DataToolbar id="data-toolbar" className="DataToolbar-class">
        <DataToolbarContent className="DataToolbarContent-class">
          <DataToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
            <DataToolbarGroup variant="filter-group">
              <DataToolbarItem>
                <Select
                  variant={SelectVariant.single}
                  aria-label="Select Input"
                  onToggle={onStatusToggle}
                  onSelect={onStatusSelect}
                  selections="Running"
                  isExpanded={false}
                >
                  {statusOptions.map((option, index) => (
                    <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
                  ))}
                </Select>
              </DataToolbarItem>
              <DataToolbarItem>
                <Select
                  variant={SelectVariant.single}
                  aria-label="Select Input"
                  onToggle={onRiskToggle}
                  onSelect={onRiskSelect}
                  selections="Low"
                  isExpanded={false}
                >
                  {riskOptions.map((option, index) => (
                    <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
                  ))}
                </Select>
              </DataToolbarItem>
            </DataToolbarGroup>
          </DataToolbarToggleGroup>
        </DataToolbarContent>
      </DataToolbar>
    );
    expect(view).toMatchSnapshot();
  });

  test('DataToolbarFilter', () => {
    const filters = {
      risk: ['Low'],
      status: ['New', 'Pending']
    };

    const statusOptions = [{ value: 'Running', disabled: false }, { value: 'Cancelled', disabled: false }];

    const riskOptions = [{ value: 'Low', disabled: false }, { value: 'High', disabled: false }];

    const onStatusToggle = () => {};
    const onRiskToggle = () => {};
    const onStatusSelect = () => {};
    const onRiskSelect = () => {};
    const onDelete = () => {};

    const view = mount(
      <DataToolbar id="data-toolbar" className="DataToolbar-class" clearAllFilters={onDelete}>
        <DataToolbarContent className="DataToolbarContent-class">
          <DataToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
            <DataToolbarGroup variant="filter-group">
              <DataToolbarFilter chips={filters.status} deleteChip={onDelete} categoryName="Status">
                <Select
                  variant={SelectVariant.single}
                  aria-label="Select Input"
                  onToggle={onStatusToggle}
                  onSelect={onStatusSelect}
                  selections="Running"
                  isExpanded={false}
                >
                  {statusOptions.map((option, index) => (
                    <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
                  ))}
                </Select>
              </DataToolbarFilter>
              <DataToolbarFilter chips={filters.risk} deleteChip={onDelete} categoryName="Risk">
                <Select
                  variant={SelectVariant.single}
                  aria-label="Select Input"
                  onToggle={onRiskToggle}
                  onSelect={onRiskSelect}
                  selections="Low"
                  isExpanded={false}
                >
                  {riskOptions.map((option, index) => (
                    <SelectOption isDisabled={option.disabled} key={index} value={option.value} />
                  ))}
                </Select>
              </DataToolbarFilter>
            </DataToolbarGroup>
          </DataToolbarToggleGroup>
        </DataToolbarContent>
      </DataToolbar>
    );
    expect(view).toMatchSnapshot();
  });
});
