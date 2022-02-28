import * as React from 'react';
import { shallow } from 'enzyme';
import { SkipToContent } from '../SkipToContent';

const props = {};

test('Verify Skip To Content', () => {
  const view = shallow(<SkipToContent href="#main-content" {...props} />);
  // Add a useful assertion here.
  expect(view).toMatchSnapshot();
});

test('Verify Skip To Content if forced to display', () => {
  const view = shallow(<SkipToContent href="#main-content" {...props} show />);
  // Add a useful assertion here.
  expect(view).toMatchSnapshot();
});
