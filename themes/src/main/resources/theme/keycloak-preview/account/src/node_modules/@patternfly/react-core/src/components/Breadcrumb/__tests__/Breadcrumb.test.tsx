import * as React from 'react';
import { Breadcrumb } from '../Breadcrumb';
import { BreadcrumbItem } from '../BreadcrumbItem';
import { mount } from 'enzyme';

describe('Breadcrumb component', () => {
  test('should render default breadcrumb', () => {
    const view = mount(<Breadcrumb />);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumb with className', () => {
    const view = mount(<Breadcrumb className="className" />);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumb with aria-label', () => {
    const view = mount(<Breadcrumb aria-label="custom label" />);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumb with children', () => {
    const view = mount(
      <Breadcrumb>
        <BreadcrumbItem to="#">Item 1</BreadcrumbItem> <BreadcrumbItem to="#">Item 1</BreadcrumbItem>
      </Breadcrumb>
    );
    expect(view).toMatchSnapshot();
  });
});
