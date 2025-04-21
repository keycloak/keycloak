import React from 'react';
import { render } from '@testing-library/react';
import { Skeleton } from '../Skeleton';

describe('skeleton', () => {
  test('default', () => {
    const { asFragment } = render(<Skeleton />);
    expect(asFragment()).toMatchSnapshot();
  });

  ['25%', '33%', '50%', '66%', '75%'].forEach((width: string) =>
    test(`skeleton with ${width} width`, () => {
      const { asFragment } = render(<Skeleton width={width} />);
      expect(asFragment()).toMatchSnapshot();
    })
  );

  ['25%', '33%', '50%', '66%', '75%', '100%'].forEach((height: string) =>
    test(`skeleton with ${height} height`, () => {
      const { asFragment } = render(<Skeleton height={height} />);
      expect(asFragment()).toMatchSnapshot();
    })
  );

  ['sm', 'md', 'lg', 'xl', '2xl', '3xl', '4xl'].forEach((fontSize: string) =>
    test(`skeleton with ${fontSize} font size`, () => {
      const { asFragment } = render(
        <Skeleton fontSize={fontSize as 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl'} />
      );
      expect(asFragment()).toMatchSnapshot();
    })
  );

  test('circle skeleton', () => {
    const { asFragment } = render(<Skeleton shape="circle" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('square skeleton', () => {
    const { asFragment } = render(<Skeleton shape="square" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
