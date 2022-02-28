import React from 'react';
import { shallow } from 'enzyme';
import { Tab } from '../Tab';

test('should not render anything', () => {
  const view = shallow(
    <Tab eventKey={0} title="Tab item 1">
      Tab 1 section
    </Tab>
  );
  expect(view).toMatchSnapshot();
});
