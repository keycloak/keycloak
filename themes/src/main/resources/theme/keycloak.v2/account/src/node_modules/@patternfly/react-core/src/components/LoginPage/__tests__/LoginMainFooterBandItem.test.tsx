import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { LoginMainFooterBandItem } from '../LoginMainFooterBandItem';

describe('LoginMainFooterBandItem', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<LoginMainFooterBandItem />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<LoginMainFooterBandItem className="extra-class">test</LoginMainFooterBandItem>);
    expect(screen.getByText('test')).toHaveClass('extra-class');
  });

  test('with custom node', () => {
    const CustomNode = () => <div>My custom node</div>;

    render(
      <LoginMainFooterBandItem>
        <CustomNode />
      </LoginMainFooterBandItem>
    );

    expect(screen.getByText('My custom node')).toBeInTheDocument();
  });
});
