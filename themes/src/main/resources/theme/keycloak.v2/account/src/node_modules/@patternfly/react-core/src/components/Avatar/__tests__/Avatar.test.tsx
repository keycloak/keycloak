import * as React from 'react';
import { render } from '@testing-library/react';
import { Avatar } from '../Avatar';

test('simple avatar', () => {
  const { asFragment } = render(<Avatar alt="avatar" src="test.png" border="light" />);
  expect(asFragment()).toMatchSnapshot();
});
