import React from 'react';
import { render } from '@testing-library/react';
import { ToolbarItem } from '../ToolbarItem';

describe('ToolbarItem', () => {
  it('should map width breakpoints', () => {
    const widths = {
      default: '100px',
      sm: '80px',
      md: '150px',
      lg: '200px',
      xl: '250px',
      '2xl': '300px'
    };

    const { asFragment } = render(<ToolbarItem widths={widths}>Test</ToolbarItem>);
    expect(asFragment()).toMatchSnapshot();
  });
});
