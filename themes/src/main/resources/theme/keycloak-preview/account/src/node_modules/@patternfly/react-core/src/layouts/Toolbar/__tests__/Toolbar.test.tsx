import * as React from 'react';
import { mount } from 'enzyme';
import { Toolbar } from '../Toolbar';
import { ToolbarGroup } from '../ToolbarGroup';
import { ToolbarItem } from '../ToolbarItem';
import { ToolbarSection } from '../ToolbarSection';

test('Simple toolbar with single group', () => {
  const view = mount(
    <Toolbar>
      <ToolbarGroup>
        <ToolbarItem>Item 1</ToolbarItem>
        <ToolbarItem>Item 2</ToolbarItem>
      </ToolbarGroup>
    </Toolbar>
  );
  expect(view).toMatchSnapshot();
});

test('Toolbar with multiple groups', () => {
  const view = mount(
    <Toolbar>
      <ToolbarGroup>
        <ToolbarItem>Item 1</ToolbarItem>
        <ToolbarItem>Item 2</ToolbarItem>
      </ToolbarGroup>
      <ToolbarGroup>
        <ToolbarItem>Item 3</ToolbarItem>
      </ToolbarGroup>
      <ToolbarGroup>
        <ToolbarItem>Item 4</ToolbarItem>
        <ToolbarItem>Item 5</ToolbarItem>
        <ToolbarItem>Item 6</ToolbarItem>
      </ToolbarGroup>
    </Toolbar>
  );
  expect(view).toMatchSnapshot();
});

test('Toolbar with sections', () => {
  const view = mount(
    <Toolbar>
      <ToolbarSection aria-label="First section">
        <ToolbarGroup>
          <ToolbarItem>Item 1</ToolbarItem>
          <ToolbarItem>Item 2</ToolbarItem>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarItem>Item 3</ToolbarItem>
        </ToolbarGroup>
      </ToolbarSection>
      <ToolbarSection aria-label="Second section">
        <ToolbarGroup>
          <ToolbarItem>Item 4</ToolbarItem>
          <ToolbarItem>Item 5</ToolbarItem>
          <ToolbarItem>Item 6</ToolbarItem>
        </ToolbarGroup>
      </ToolbarSection>
    </Toolbar>
  );
  expect(view).toMatchSnapshot();
});
