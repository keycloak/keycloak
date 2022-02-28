import React from 'react';
import { render, mount } from 'enzyme';
import { Tabs } from '../Tabs';
import { Tab } from '../Tab';

test('should render simple tabs', () => {
  const view = render(
    <Tabs id="simpleTabs">
      <Tab id="tab1" eventKey={0} title="Tab item 1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3">
        Tab 3 section
      </Tab>
      <Tab
        id="tab4"
        eventKey={3}
        title={
          <b>
            Tab item <i>4</i>
          </b>
        }
      >
        Tab 4 section
      </Tab>
    </Tabs>
  );
  expect(view).toMatchSnapshot();
});

test('should render accessible tabs', () => {
  const view = render(
    <Tabs id="accessibleTabs" aria-label="accessible Tabs example" variant="nav">
      <Tab id="tab1" eventKey={0} title="Tab item 1" href="#/items/1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2" href="#/items/2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3" href="#/items/3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  expect(view).toMatchSnapshot();
});

test('should render filled tabs', () => {
  const view = render(
    <Tabs id="filledTabs" isFilled>
      <Tab id="tab1" eventKey={0} title="Tab item 1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  expect(view).toMatchSnapshot();
});

test('should render secondary tabs', () => {
  const view = render(
    <Tabs id="primarieTabs">
      <Tab eventKey={0} title="Tab item 1">
        <Tabs id="secondaryTabs">
          <Tab id="secondary tab1" eventKey={10} title="Secondary Tab 1">
            Secondary Tab 1 section
          </Tab>
          <Tab id="secondary tab2" eventKey={11} title="Secondary Tab 2">
            Secondary Tab 2 section
          </Tab>
          <Tab id="secondary tab3" eventKey={12} title="Secondary Tab 3">
            Secondary Tab 3 section
          </Tab>
        </Tabs>
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  expect(view).toMatchSnapshot();
});

test('should call scrollLeft tabs with scrolls', () => {
  const view = mount(
    <Tabs id="scrollLeft" isFilled>
      <Tab id="tab1" eventKey={0} title="Tab item 1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  view
    .find('.pf-c-tabs__scroll-button')
    .first()
    .simulate('click');
  expect(view).toMatchSnapshot();
});

test('should call scrollRight tabs with scrolls', () => {
  const view = mount(
    <Tabs id="scrollRight" isFilled>
      <Tab id="tab1" eventKey={0} title="Tab item 1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  view
    .find('.pf-c-tabs__scroll-button')
    .last()
    .simulate('click');
  expect(view).toMatchSnapshot();
});

test('should call handleScrollButtons tabs with scrolls', () => {
  const view = mount(
    <Tabs id="handleScrollButtons" isFilled>
      <Tab id="tab1" eventKey={0} title="Tab item 1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={1} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={2} title="Tab item 3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  view.simulate('scroll');
  expect(view).toMatchSnapshot();
});

test('should render tabs with eventKey Strings', () => {
  const view = render(
    <Tabs id="eventKeyTabs">
      <Tab id="tab1" eventKey={'one'} title="Tab item 1">
        Tab 1 section
      </Tab>
      <Tab id="tab2" eventKey={'two'} title="Tab item 2">
        Tab 2 section
      </Tab>
      <Tab id="tab3" eventKey={'three'} title="Tab item 3">
        Tab 3 section
      </Tab>
    </Tabs>
  );
  expect(view).toMatchSnapshot();
});
