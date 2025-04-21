import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DataList } from '../DataList';
import { DataListItem } from '../DataListItem';
import { DataListAction } from '../DataListAction';
import { DataListCell } from '../DataListCell';
import { DataListToggle } from '../DataListToggle';
import { DataListItemCells } from '../DataListItemCells';
import { DataListItemRow } from '../DataListItemRow';
import { DataListContent } from '../DataListContent';
import { Button } from '../../Button';
import { DropdownItem, Dropdown, KebabToggle, DropdownPosition } from '../../Dropdown';

describe('DataList', () => {
  test('List default', () => {
    const { asFragment } = render(<DataList aria-label="this is a simple list" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('List compact', () => {
    const { asFragment } = render(<DataList aria-label="this is a simple list" isCompact />);
    expect(asFragment()).toMatchSnapshot();
  });

  describe('DataList variants', () => {
    ['none', 'always', 'sm', 'md', 'lg', 'xl', '2xl'].forEach(oneBreakpoint => {
      test(`Breakpoint - ${oneBreakpoint}`, () => {
        const { asFragment } = render(
          <DataList aria-label="this is a simple list" gridBreakpoint={oneBreakpoint as any} />
        );
        expect(asFragment()).toMatchSnapshot();
      });
    });
  });

  test('List draggable', () => {
    const { asFragment } = render(<DataList aria-label="this is a simple list" isCompact onDragFinish={jest.fn()} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('List', () => {
    const { asFragment } = render(
      <DataList key="list-id-1" className="data-list-custom" aria-label="this is a simple list" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('List renders with a hidden input to improve a11y when selectableRow is passed', () => {
    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'multiple', onChange: () => {} }}>
        <DataListItem>
          <DataListItemRow aria-labelledby="test-id">
            <p id="test-id">Test</p>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toBeInTheDocument();
  });

  test('List does not render with a hidden input to improve a11y when selectableRow is not passed', () => {
    render(
      <DataList aria-label="this is a simple list">
        <DataListItem>
          <DataListItemRow aria-labelledby="test-id">
            <p id="test-id">Test</p>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    );

    const selectableInput = screen.queryByRole('checkbox', { hidden: true });

    expect(selectableInput).not.toBeInTheDocument();
  });

  test('List hidden input renders as a radio when selectableRow.type is radio', () => {
    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'single', onChange: () => {} }}>
        <DataListItem>
          <DataListItemRow aria-labelledby="test-id">
            <p id="test-id">Test</p>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    );

    const selectableRadioInput = screen.getByRole('radio', { hidden: true });
    const selectableCheckboxInput = screen.queryByRole('checkbox', { hidden: true });

    expect(selectableRadioInput).toBeInTheDocument();
    expect(selectableCheckboxInput).not.toBeInTheDocument();
  });

  test('List calls selectableRow.onChange when the selectable input changes', () => {
    const mock = jest.fn();

    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'multiple', onChange: mock }}>
        <DataListItem>
          <DataListItemRow aria-labelledby="test-id">
            <p id="test-id">Test</p>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });
    userEvent.click(selectableInput);

    expect(mock).toHaveBeenCalled();
  });

  test('List does not call selectableRow.onChange when the selectable input is not changed', () => {
    const mock = jest.fn();

    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'multiple', onChange: mock }}>
        <DataListItem>
          <DataListItemRow aria-labelledby="test-id">
            <p id="test-id">Test</p>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    );

    expect(mock).not.toHaveBeenCalled();
  });

  test('Item applies selectableInputAriaLabel to the hidden input', () => {
    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'multiple', onChange: () => {} }}>
        <DataListItem selectableInputAriaLabel="Data list item label test">
          <DataListItemRow aria-labelledby="test-id">
            <p id="test-id">Test</p>
          </DataListItemRow>
        </DataListItem>
      </DataList>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toHaveAccessibleName('Data list item label test');
  });

  test('Item defaults to labelling its input using its aria-labelledby prop', () => {
    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'multiple', onChange: () => {} }}>
        <DataListItem aria-labelledby="test-id">
          <p id="test-id">Test cell content</p>
        </DataListItem>
      </DataList>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toHaveAccessibleName('Test cell content');
  });

  test('Item prioritizes selectableInputAriaLabel over aria-labelledby prop', () => {
    render(
      <DataList aria-label="this is a simple list" selectableRow={{ type: 'multiple', onChange: () => {} }}>
        <DataListItem aria-labelledby="test-id" selectableInputAriaLabel="Data list item label test">
          <p id="test-id">Test cell content</p>
        </DataListItem>
      </DataList>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toHaveAccessibleName('Data list item label test');
  });

  test('Item default', () => {
    const { asFragment } = render(
      <DataListItem key="item-id-1" aria-labelledby="item-1">
        test
      </DataListItem>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Item expanded', () => {
    render(
      <DataListItem aria-labelledby="item-1" isExpanded>
        test
      </DataListItem>
    );
    expect(screen.getByRole('listitem')).toHaveClass('pf-c-data-list__item pf-m-expanded');
  });

  test('Item', () => {
    const { asFragment } = render(
      <DataListItem className="data-list-item-custom" aria-labelledby="item-1">
        test
      </DataListItem>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('item row default', () => {
    const { asFragment } = render(<DataListItemRow>test</DataListItemRow>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Cell default', () => {
    const { asFragment } = render(<DataListCell>Secondary</DataListCell>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Cells', () => {
    const { asFragment } = render(
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
    expect(asFragment()).toMatchSnapshot();
  });

  test('Cell with width modifier', () => {
    [
      { width: 1, class: '' },
      { width: 2, class: 'pf-m-flex-2' },
      { width: 3, class: 'pf-m-flex-3' },
      { width: 4, class: 'pf-m-flex-4' },
      { width: 5, class: 'pf-m-flex-5' }
    ].forEach((testCase, index) => {
      const testId = `cell-test-id-${index}`;

      render(
        <DataListCell data-testid={testId} width={testCase.width as any} key={index}>
          Primary Id
        </DataListCell>
      );

      const dataListCell = screen.getByTestId(testId);

      testCase.class === ''
        ? expect(dataListCell).toHaveClass('pf-c-data-list__cell')
        : expect(dataListCell).toHaveClass(`pf-c-data-list__cell ${testCase.class}`);
    });
  });

  test('Cell with text modifiers', () => {
    [
      { wrapModifier: null as any, class: '' },
      { wrapModifier: 'breakWord', class: 'pf-m-break-word' },
      { wrapModifier: 'nowrap', class: 'pf-m-nowrap' },
      { wrapModifier: 'truncate', class: 'pf-m-truncate' }
    ].forEach((testCase, index) => {
      const testId = `cell-test-id-${index}`;

      render(
        <DataListCell data-testid={testId} wrapModifier={testCase.wrapModifier} key={index}>
          Primary Id
        </DataListCell>
      );

      const dataListCell = screen.getByTestId(testId);

      testCase.class === ''
        ? expect(dataListCell).toHaveClass('pf-c-data-list__cell')
        : expect(dataListCell).toHaveClass(`pf-c-data-list__cell ${testCase.class}`);
    });
  });

  test('Toggle default with aria label', () => {
    render(<DataListToggle aria-label="Toggle details for" id="ex-toggle2" />);

    expect(screen.getByRole('button')).not.toHaveAttribute('aria-labelledby');
    expect(screen.getByRole('button')).toHaveAttribute('aria-label', 'Toggle details for');
    expect(screen.getByRole('button')).toHaveAttribute('aria-expanded', 'false');
  });

  test('Toggle expanded', () => {
    render(<DataListToggle aria-label="Toggle details for" id="ex-toggle2" isExpanded />);
    expect(screen.getByRole('button')).toHaveAttribute('aria-expanded', 'true');
  });

  test('DataListAction dropdown', () => {
    const { asFragment } = render(
      <DataListAction aria-label="Actions" aria-labelledby="ex-action" id="ex-action" isPlainButtonAction>
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
    expect(asFragment()).toMatchSnapshot();
  });

  test('DataListAction button', () => {
    const { asFragment } = render(
      <DataListAction aria-label="Actions" aria-labelledby="ex-action" id="ex-action">
        <Button id="delete-item-1">Delete</Button>
      </DataListAction>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('DataListAction visibility - show button when lg', () => {
    render(
      <DataListAction
        visibility={{ default: 'hidden', lg: 'visible' }}
        aria-labelledby="check-action-item2 check-action-action2"
        id="check-action-action2"
        aria-label="Actions"
      >
        <Button variant="primary">Primary</Button>
      </DataListAction>
    );

    expect(screen.getByRole('button').parentElement).toHaveClass('pf-m-hidden');
    expect(screen.getByRole('button').parentElement).toHaveClass('pf-m-visible-on-lg');
  });

  test('DataListAction visibility - hide button on 2xl', () => {
    render(
      <DataListAction
        visibility={{ '2xl': 'hidden' }}
        aria-labelledby="check-action-item2 check-action-action2"
        id="check-action-action2"
        aria-label="Actions"
      >
        <Button variant="primary">Primary</Button>
      </DataListAction>
    );

    expect(screen.getByRole('button').parentElement).toHaveClass('pf-m-hidden-on-2xl');
  });

  test('DataListContent', () => {
    const { asFragment } = render(<DataListContent aria-label="Primary Content Details"> test</DataListContent>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('DataListContent hasNoPadding', () => {
    const { asFragment } = render(
      <DataListContent aria-label="Primary Content Details" hidden hasNoPadding>
        test
      </DataListContent>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
