import React from 'react';
import { render } from '@testing-library/react';
import { BackToTop } from '../BackToTop';

describe('BackToTop', () => {
  test('verify basic', () => {
    const { asFragment } = render(<BackToTop />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('verify custom class', () => {
    const { asFragment } = render(<BackToTop className="custom-css">test</BackToTop>);

    expect(asFragment()).toMatchSnapshot();
  });

  test('verify always show', () => {
    const { asFragment } = render(<BackToTop isAlwaysVisible>test</BackToTop>);

    expect(asFragment()).toMatchSnapshot();
  });
});
