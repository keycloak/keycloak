import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxCloseButton } from '../AboutModalBoxCloseButton';

test('AboutModalBoxCloseButton Test', () => {
  const view = shallow(<AboutModalBoxCloseButton />);
  expect(view).toMatchSnapshot();
});

test('AboutModalBoxCloseButton Test onclose', () => {
  const onClose = jest.fn();
  const view = shallow(<AboutModalBoxCloseButton onClose={onClose} />);
  expect(view).toMatchSnapshot();
});

test('AboutModalBoxCloseButton Test close button aria label', () => {
  const closeButtonAriaLabel = 'Klose Daylok';
  const view = shallow(<AboutModalBoxCloseButton aria-label={closeButtonAriaLabel} />);
  expect(view).toMatchSnapshot();
});
