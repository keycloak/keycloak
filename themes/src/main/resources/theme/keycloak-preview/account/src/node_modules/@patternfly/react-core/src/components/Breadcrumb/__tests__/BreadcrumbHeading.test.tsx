import * as React from 'react';
import { BreadcrumbHeading } from '../BreadcrumbHeading';
import { shallow } from 'enzyme';

describe('BreadcrumbHeading component', () => {
  test('should render default breadcrumbHeading', () => {
    const view = shallow(<BreadcrumbHeading>Item</BreadcrumbHeading>);
    expect(view).toMatchSnapshot();
  });

  test('should pass classname', () => {
    const view = shallow(<BreadcrumbHeading className="Class">Item</BreadcrumbHeading>);
    expect(view).toMatchSnapshot();
  });

  test('should pass custom id', () => {
    const view = shallow(<BreadcrumbHeading id="Id">Item</BreadcrumbHeading>);
    expect(view).toMatchSnapshot();
  });

  test('should render link breadcrumbTitle', () => {
    const view = shallow(<BreadcrumbHeading to="/somewhere">Item</BreadcrumbHeading>);
    expect(view).toMatchSnapshot();
  });

  test('should render breadcrumbHeading with target', () => {
    const view = shallow(
      <BreadcrumbHeading to="#here" target="_blank">
        Item
      </BreadcrumbHeading>
    );
    expect(view).toMatchSnapshot();
  });
});
