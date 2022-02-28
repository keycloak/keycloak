import React from 'react';
import { shallow, mount } from 'enzyme';
import { DataList } from '../DataList';
import { DataListItem } from '../DataListItem';
import { DataListAction, DataListActionVisibility } from '../DataListAction';
import { DataListCell } from '../DataListCell';
import { DataListToggle } from '../DataListToggle';
import { DataListItemCells } from '../DataListItemCells';
import { DataListItemRow } from '../DataListItemRow';
import { DataListContent } from '../DataListContent';
import { Button } from '../../Button';
import { css } from '@patternfly/react-styles';
import { DropdownItem, Dropdown, KebabToggle, DropdownPosition } from '../../Dropdown';

describe('DataList', () => {
  test('List default', () => {
    const view = shallow(<DataList aria-label="this is a simple list" />);
    expect(view).toMatchSnapshot();
  });

  test('List compact', () => {
    const view = shallow(<DataList aria-label="this is a simple list" isCompact />);
    expect(view).toMatchSnapshot();
  });

  test('List', () => {
    const view = shallow(<DataList key="list-id-1" className="data-list-custom" aria-label="this is a simple list" />);
    expect(view).toMatchSnapshot();
  });

  test('Item default', () => {
    const view = shallow(
      <DataListItem key="item-id-1" aria-labelledby="item-1">
        test
      </DataListItem>
    );
    expect(view).toMatchSnapshot();
  });

  test('Item expanded', () => {
    const view = mount(
      <DataListItem aria-labelledby="item-1" isExpanded>
        test
      </DataListItem>
    );
    expect(view.find('li').props().className).toBe('pf-c-data-list__item pf-m-expanded');
  });

  test('Item', () => {
    const view = shallow(
      <DataListItem className="data-list-item-custom" aria-labelledby="item-1">
        test
      </DataListItem>
    );
    expect(view).toMatchSnapshot();
  });

  test('item row default', () => {
    const view = shallow(<DataListItemRow>test</DataListItemRow>);
    expect(view).toMatchSnapshot();
  });

  test('Cell default', () => {
    const view = shallow(<DataListCell>Secondary</DataListCell>);
    expect(view).toMatchSnapshot();
  });

  test('Cells', () => {
    const view = shallow(
      <DataListItemCells
        dataListCells={[
          <DataListCell key="list-id-1" id="primary-item" className="data-list-custom">
            Primary Id
          </DataListCell>,
          <DataListCell key="list-id-2" id="primary-item" className="data-list-custom">
            Primary Id 2
          </DataListCell>
        ]}
      />
    );
    expect(view).toMatchSnapshot();
  });

  test('Cell with width modifier', () => {
    [
      { width: 1 as const, class: '' },
      { width: 2 as const, class: 'pf-m-flex-2' },
      { width: 3 as const, class: 'pf-m-flex-3' },
      { width: 4 as const, class: 'pf-m-flex-4' },
      { width: 5 as const, class: 'pf-m-flex-5' }
    ].forEach(testCase => {
      const view = shallow(
        <DataListCell width={testCase.width} key="list-id-1" id="primary-item">
          Primary Id
        </DataListCell>
      );
      testCase.class === ''
        ? expect(view.props().className).toBe('pf-c-data-list__cell')
        : expect(view.props().className).toBe(`pf-c-data-list__cell ${testCase.class}`);
    });
  });

  test('Toggle default with aria label', () => {
    const view = shallow(
      <DataListToggle aria-label="Toggle details for" aria-labelledby="ex-toggle2 ex-item2" id="ex-toggle2" />
    );

    expect(view.find(Button).props()['aria-label']).toBe('Toggle details for');
    expect(view.find(Button).props()['aria-labelledby']).toBe(null);
    expect(view.find(Button).props()['aria-expanded']).toBe(false);
    expect(view.find(Button).props().id).toBe('ex-toggle2');
    expect(view.find(Button).props().id).toBe('ex-toggle2');
  });

  test('Toggle expanded', () => {
    const view = shallow(<DataListToggle aria-label="Toggle details for" id="ex-toggle2" isExpanded />);
    expect(view.find(Button).props()['aria-expanded']).toBe(true);
  });

  test('DataListAction dropdown', () => {
    const view = shallow(
      <DataListAction aria-label="Actions" aria-labelledby="ex-action" id="ex-action">
        <Dropdown
          isPlain
          position={DropdownPosition.right}
          toggle={<KebabToggle />}
          dropdownItems={[
            <DropdownItem component="button" onClick={jest.fn()} key="action-1">
              action-1
            </DropdownItem>,
            <DropdownItem component="button" onClick={jest.fn()} key="action-2">
              action-2
            </DropdownItem>
          ]}
        />
      </DataListAction>
    );
    expect(view).toMatchSnapshot();
  });

  test('DataListAction button', () => {
    const view = shallow(
      <DataListAction aria-label="Actions" aria-labelledby="ex-action" id="ex-action">
        <Button id="delete-item-1">Delete</Button>
      </DataListAction>
    );
    expect(view).toMatchSnapshot();
  });

  test('DataListAction visibility - show button when lg', () => {
    const view = shallow(
      <DataListAction
        className={css(DataListActionVisibility.visibleOnLg, DataListActionVisibility.hidden)}
        aria-labelledby="check-action-item2 check-action-action2"
        id="check-action-action2"
        aria-label="Actions"
      >
        <Button variant="primary">Primary</Button>
      </DataListAction>
    );
    expect(view.find('div').props().className).toContain('pf-m-hidden');
    expect(view.find('div').props().className).toContain('pf-m-visible-on-lg');
  });

  test('DataListAction visibility - hide button on 2xl', () => {
    const view = shallow(
      <DataListAction
        className={css(DataListActionVisibility.hiddenOn2Xl)}
        aria-labelledby="check-action-item2 check-action-action2"
        id="check-action-action2"
        aria-label="Actions"
      >
        <Button variant="primary">Primary</Button>
      </DataListAction>
    );
    expect(view.find('div').props().className).toContain('pf-m-hidden-on-2xl');
  });

  test('DataListContent', () => {
    const view = shallow(<DataListContent aria-label="Primary Content Details"> test</DataListContent>);
    expect(view).toMatchSnapshot();
  });

  test('DataListContent noPadding', () => {
    const view = shallow(
      <DataListContent aria-label="Primary Content Details" hidden noPadding>
        test
      </DataListContent>
    );
    expect(view).toMatchSnapshot();
  });
});
