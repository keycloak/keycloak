import React from 'react';

import { render, screen } from '@testing-library/react';

import BookOpen from '@patternfly/react-icons/dist/esm/icons/book-open-icon';
import Key from '@patternfly/react-icons/dist/esm/icons/key-icon';
import Desktop from '@patternfly/react-icons/dist/esm/icons/desktop-icon';
import { List, ListVariant, ListComponent, OrderType } from '../List';
import { ListItem } from '../ListItem';

const ListItems = () => (
  <List>
    <ListItem>First</ListItem>
    <ListItem>Second</ListItem>
    <ListItem>Third</ListItem>
  </List>
);

describe('List', () => {
  test('simple list', () => {
    const { asFragment } = render(
      <List>
        <ListItems />
      </List>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('inline list', () => {
    const { asFragment } = render(
      <List variant={ListVariant.inline}>
        <ListItems />
      </List>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('ordered list', () => {
    const { asFragment } = render(
      <List component={ListComponent.ol}>
        <ListItem>Apple</ListItem>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('ordered list starts with 2nd item', () => {
    render(
      <List component={ListComponent.ol} start={2}>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(screen.getByRole('list')).toHaveAttribute('start', '2');
  });

  test('ordered list items will be numbered with uppercase letters', () => {
    render(
      <List component={ListComponent.ol} type={OrderType.uppercaseLetter}>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(screen.getByRole('list')).toHaveAttribute('type', 'A');
  });

  test('inlined ordered list', () => {
    render(
      <List variant={ListVariant.inline} component={ListComponent.ol}>
        <ListItem>Apple</ListItem>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(screen.getByRole('list')).toHaveClass('pf-m-inline');
  });

  test('bordered list', () => {
    render(
      <List isBordered>
        <ListItems />
      </List>
    );
    expect(screen.getAllByRole('list')[0]).toHaveClass('pf-m-bordered');
  });

  test('plain list', () => {
    render(
      <List isPlain>
        <ListItems />
      </List>
    );
    expect(screen.getAllByRole('list')[0]).toHaveClass('pf-m-plain');
  });

  test('icon list', () => {
    const { asFragment } = render(
      <List isPlain>
        <ListItem icon={<BookOpen />}>Apple</ListItem>
        <ListItem icon={<Key />}>Banana</ListItem>
        <ListItem icon={<Desktop />}>Orange</ListItem>
      </List>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('icon large list', () => {
    const { asFragment } = render(
      <List iconSize="large">
        <ListItem icon={<BookOpen />}>Apple</ListItem>
        <ListItem icon={<Key />}>Banana</ListItem>
        <ListItem icon={<Desktop />}>Orange</ListItem>
      </List>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
