import * as React from 'react';
import { BreadcrumbItem } from '../BreadcrumbItem';
import { render } from '@testing-library/react';

describe('BreadcrumbItem component', () => {
  test('should render default breadcrumbItem', () => {
    const { asFragment } = render(<BreadcrumbItem>Item</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumbItem with className', () => {
    const { asFragment } = render(<BreadcrumbItem className="Class">Item</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumbItem with id', () => {
    const { asFragment } = render(<BreadcrumbItem id="Item 1">Item</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render active breadcrumbItem', () => {
    const { asFragment } = render(<BreadcrumbItem isActive>Item</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render link breadcrumbItem', () => {
    const { asFragment } = render(<BreadcrumbItem to="/somewhere">Item</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumbItem with target', () => {
    const { asFragment } = render(<BreadcrumbItem target="/somewhere">Item</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumbItem with custom element', () => {
    const { asFragment } = render(
      <BreadcrumbItem>
        <h1>Header</h1>
      </BreadcrumbItem>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render breadcrumbItem with component', () => {
    const MyComponent = () => <div>My component contents</div>;
    const consoleError = jest.spyOn(console, 'error').mockImplementation();
    const { asFragment } = render(<BreadcrumbItem component={MyComponent}>Stuff</BreadcrumbItem>);
    expect(asFragment()).toMatchSnapshot();
    expect(consoleError).not.toHaveBeenCalled();
  });
});
