import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SimpleList } from '../SimpleList';
import { SimpleListGroup } from '../SimpleListGroup';
import { SimpleListItem } from '../SimpleListItem';

const items = [
  <SimpleListItem key="i1">Item 1</SimpleListItem>,
  <SimpleListItem key="i2">Item 2</SimpleListItem>,
  <SimpleListItem key="i3">Item 3</SimpleListItem>
];

const anchors = [
  <SimpleListItem key="i1" component="a">
    Item 1
  </SimpleListItem>,
  <SimpleListItem key="i2" component="a">
    Item 2
  </SimpleListItem>,
  <SimpleListItem key="i3" component="a">
    Item 3
  </SimpleListItem>
];

describe('SimpleList', () => {
  test('renders content', () => {
    const { asFragment } = render(<SimpleList>{items}</SimpleList>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders grouped content', () => {
    const { asFragment } = render(
      <SimpleList>
        <SimpleListGroup title="Group 1">{items}</SimpleListGroup>
      </SimpleList>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('onSelect is called when item is selected', () => {
    const onSelect = jest.fn();

    render(<SimpleList onSelect={onSelect}>{items}</SimpleList>);

    userEvent.click(screen.getByText('Item 1'));
    expect(onSelect).toHaveBeenCalled();
  });

  test('renders anchor content', () => {
    render(<SimpleList>{anchors}</SimpleList>);
    expect(screen.getAllByRole('link').length).toEqual(3);
  });

  test('onSelect is called when anchor item is selected', () => {
    const onSelect = jest.fn();

    render(<SimpleList onSelect={onSelect}>{anchors}</SimpleList>);

    userEvent.click(screen.getByText('Item 1'));
    expect(onSelect).toHaveBeenCalled();
  });
});

describe('SimpleListGroup', () => {
  test('renders content', () => {
    render(<SimpleListGroup title="Group 1">{items}</SimpleListGroup>);
    expect(screen.getByText('Group 1')).toBeInTheDocument();
  });
});

describe('SimpleListItem', () => {
  test('renders content', () => {
    render(<SimpleListItem>Item 1</SimpleListItem>);
    expect(screen.getByText('Item 1')).toBeInTheDocument();
  });

  test('renders anchor', () => {
    render(<SimpleListItem component="a">Item 1</SimpleListItem>);
    expect(screen.getByText('Item 1')).toBeInTheDocument();
  });
});
