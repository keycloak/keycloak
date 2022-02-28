import React from 'react';
import { mount } from 'enzyme';
import { List, ListVariant, ListComponent, OrderType } from '../List';
import { ListItem } from '../ListItem';

const ListItems = () => (
  <React.Fragment>
    <List>
      <ListItem>First</ListItem>
      <ListItem>Second</ListItem>
      <ListItem>Third</ListItem>
    </List>
  </React.Fragment>
);

describe('list', () => {
  test('simple list', () => {
    const view = mount(
      <List>
        <ListItems />
      </List>
    );
    expect(view).toMatchSnapshot();
  });

  test('inline list', () => {
    const view = mount(
      <List variant={ListVariant.inline}>
        <ListItems />
      </List>
    );
    expect(view).toMatchSnapshot();
  });

  test('ordered list', () => {
    const view = mount(
      <List component={ListComponent.ol}>
        <ListItem>Apple</ListItem>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(view.find('ol').length).toBe(1);
  });

  test('ordered list starts with 2nd item', () => {
    const view = mount(
      <List component={ListComponent.ol} start={2}>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(view.find('ol').prop('start')).toBe(2);
  });

  test('ordered list items will be numbered with uppercase letters', () => {
    const view = mount(
      <List component={ListComponent.ol} type={OrderType.uppercaseLetter}>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(view.find('ol').prop('type')).toBe('A');
  });

  test('inlined ordered list', () => {
    const view = mount(
      <List variant={ListVariant.inline} component={ListComponent.ol}>
        <ListItem>Apple</ListItem>
        <ListItem>Banana</ListItem>
        <ListItem>Orange</ListItem>
      </List>
    );
    expect(view.find('ol.pf-m-inline').length).toBe(1);
  });
});
