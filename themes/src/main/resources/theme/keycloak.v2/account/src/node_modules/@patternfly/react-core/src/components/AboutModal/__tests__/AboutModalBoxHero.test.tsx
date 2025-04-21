import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalBoxHero } from '../AboutModalBoxHero';

test('test About Modal Box SHero', () => {
  const { asFragment } = render(<AboutModalBoxHero />);
  expect(asFragment()).toMatchSnapshot();
});
