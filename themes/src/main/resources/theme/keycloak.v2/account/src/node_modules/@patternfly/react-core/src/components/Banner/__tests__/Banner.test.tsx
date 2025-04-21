import { Banner } from '../Banner';
import React from 'react';
import { render } from '@testing-library/react';

['default', 'info', 'success', 'warning', 'danger'].forEach((variant: string) => {
  test(`${variant} banner`, () => {
    const { asFragment } = render(
      <Banner
        variant={variant as 'default' | 'info' | 'success' | 'warning' | 'danger'}
        aria-label={variant}
        screenReaderText={`${variant} banner`}
      >
        {variant} Banner
      </Banner>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

test(`sticky banner`, () => {
  const { asFragment } = render(
    <Banner aria-label="sticky" isSticky>
      Sticky Banner
    </Banner>
  );
  expect(asFragment()).toMatchSnapshot();
});
