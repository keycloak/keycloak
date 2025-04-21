import * as React from 'react';
import { Breadcrumb } from '../Breadcrumb';
import { BreadcrumbItem } from '../BreadcrumbItem';
import { render } from '@testing-library/react';

describe('Breadcrumb component', () => {
  test('should render default breadcrumb', () => {
    const { asFragment } = render(<Breadcrumb />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumb with className', () => {
    const { asFragment } = render(<Breadcrumb className="className" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumb with aria-label', () => {
    const { asFragment } = render(<Breadcrumb aria-label="custom label" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumb with children', () => {
    const { asFragment } = render(
      <Breadcrumb>
        <BreadcrumbItem to="#">Item 1</BreadcrumbItem> <BreadcrumbItem to="#">Item 1</BreadcrumbItem>
      </Breadcrumb>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
