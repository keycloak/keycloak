import * as React from 'react';
import { BreadcrumbHeading } from '../BreadcrumbHeading';
import { render } from '@testing-library/react';

describe('BreadcrumbHeading component', () => {
  test('should render default breadcrumbHeading', () => {
    const { asFragment } = render(<BreadcrumbHeading>Item</BreadcrumbHeading>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should pass classname', () => {
    const { asFragment } = render(<BreadcrumbHeading className="Class">Item</BreadcrumbHeading>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should pass custom id', () => {
    const { asFragment } = render(<BreadcrumbHeading id="Id">Item</BreadcrumbHeading>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render link breadcrumbTitle', () => {
    const { asFragment } = render(<BreadcrumbHeading to="/somewhere">Item</BreadcrumbHeading>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumbHeading with target', () => {
    const { asFragment } = render(
      <BreadcrumbHeading to="#here" target="_blank">
        Item
      </BreadcrumbHeading>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
