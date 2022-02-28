import * as React from 'react';
import { BreadcrumbItem } from '../BreadcrumbItem';
import { shallow } from 'enzyme';

describe('BreadcrumbItem component', () => {
  test('should render default breadcrumbItem', () => {
    const view = shallow(<BreadcrumbItem>Item</BreadcrumbItem>);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumbItem with className', () => {
    const view = shallow(<BreadcrumbItem className="Class">Item</BreadcrumbItem>);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumbItem with id', () => {
    const view = shallow(<BreadcrumbItem id="Item 1">Item</BreadcrumbItem>);
    expect(view).toMatchSnapshot();
  });

  test('should render active breadcrumbItem', () => {
    const view = shallow(<BreadcrumbItem isActive>Item</BreadcrumbItem>);
    expect(view).toMatchSnapshot();
  });

  test('should render link breadcrumbItem', () => {
    const view = shallow(<BreadcrumbItem to="/somewhere">Item</BreadcrumbItem>);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumbItem with target', () => {
    const view = shallow(<BreadcrumbItem target="/somewhere">Item</BreadcrumbItem>);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumbItem with custom element', () => {
    const view = shallow(
      <BreadcrumbItem>
        <h1>Header</h1>
      </BreadcrumbItem>
    );
    expect(view).toMatchSnapshot();
  });
});
