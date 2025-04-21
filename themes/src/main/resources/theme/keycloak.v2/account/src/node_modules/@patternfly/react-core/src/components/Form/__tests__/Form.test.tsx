import React from 'react';
import { render } from '@testing-library/react';
import { Form } from '../Form';

describe('Form component', () => {
  test('should render default form variant', () => {
    const { asFragment } = render(<Form />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render horizontal form variant', () => {
    const { asFragment } = render(<Form isHorizontal />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form with limited width', () => {
    const { asFragment } = render(<Form isWidthLimited />);
    expect(asFragment()).toMatchSnapshot();
  });
});
