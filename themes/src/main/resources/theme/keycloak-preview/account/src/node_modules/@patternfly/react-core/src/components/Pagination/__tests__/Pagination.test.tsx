import React from 'react';
import { mount } from 'enzyme';
import { Pagination, PaginationVariant } from '../index';

describe('component render', () => {
  test('should render correctly top', () => {
    const wrapper = mount(<Pagination itemCount={20} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('should render correctly bottom', () => {
    const wrapper = mount(<Pagination itemCount={20} variant={PaginationVariant.bottom} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('should render correctly compact', () => {
    const wrapper = mount(<Pagination itemCount={20} isCompact />);
    expect(wrapper).toMatchSnapshot();
  });

  test('should render correctly disabled', () => {
    const wrapper = mount(<Pagination itemCount={20} isDisabled />);
    expect(wrapper).toMatchSnapshot();
  });

  test('limited number of pages', () => {
    const wrapper = mount(<Pagination itemCount={20} perPage={20} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('zero results', () => {
    const wrapper = mount(<Pagination itemCount={0} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('last page', () => {
    const wrapper = mount(<Pagination itemCount={20} perPage={10} page={2} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('custom perPageOptions', () => {
    const wrapper = mount(<Pagination itemCount={40} perPageOptions={[{ title: 'some', value: 1 }]} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('empty per page options', () => {
    const wrapper = mount(<Pagination itemCount={40} perPageOptions={[]} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('no items', () => {
    const wrapper = mount(<Pagination itemCount={0} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('custom start end', () => {
    const wrapper = mount(<Pagination itemCount={40} itemsStart={5} itemsEnd={15} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('titles', () => {
    const wrapper = mount(<Pagination itemCount={40} titles={{ items: 'values', page: 'books' }} />);
    expect(wrapper).toMatchSnapshot();
  });

  test('custom pagination toggle', () => {
    const wrapper = mount(
      // eslint-disable-next-line no-template-curly-in-string
      <Pagination itemCount={40} toggleTemplate={'${firstIndex} - ${lastIndex} - ${itemCount} - ${itemsTitle}'} />
    );
    expect(wrapper).toMatchSnapshot();
  });

  test('up drop direction', () => {
    const wrapper = mount(<Pagination itemCount={40} dropDirection="up" />);
    expect(wrapper).toMatchSnapshot();
  });
});

describe('API', () => {
  describe('onSetPage', () => {
    const onSetPage = jest.fn();

    test('should call first', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} page={2} />);
      wrapper
        .find('[data-action="first"]')
        .first()
        .simulate('click');
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(1);
    });

    test('should call previous', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} page={3} />);
      wrapper
        .find('[data-action="previous"]')
        .first()
        .simulate('click');
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(2);
    });

    test('should call next', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} />);
      wrapper
        .find('[data-action="next"]')
        .first()
        .simulate('click');
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(2);
    });

    test('should call last', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} />);
      wrapper
        .find('[data-action="last"]')
        .first()
        .simulate('click');
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(4);
    });

    test('should call input', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} />);
      wrapper
        .find('input')
        .first()
        .simulate('change', { target: { value: '1' } })
        .simulate('keydown', { keyCode: 13 });
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(1);
    });

    test('should call input wrong value', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} />);
      wrapper
        .find('input')
        .first()
        .simulate('change', { target: { value: 'a' } })
        .simulate('keydown', { keyCode: 13 });
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(1);
    });

    test('should call input huge page number', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} />);
      wrapper
        .find('input')
        .first()
        .simulate('change', { target: { value: '10' } })
        .simulate('keydown', { keyCode: 13 });
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(4);
    });

    test('should call input small page number', () => {
      const wrapper = mount(<Pagination onSetPage={onSetPage} itemCount={40} />);
      wrapper
        .find('input')
        .first()
        .simulate('change', { target: { value: '-10' } })
        .simulate('keydown', { keyCode: 13 });
      expect(onSetPage.mock.calls).toHaveLength(1);
      expect(onSetPage.mock.calls[0][1]).toBe(1);
    });

    test('should NOT call', () => {
      const wrapper = mount(<Pagination itemCount={40} />);
      wrapper
        .find('[data-action="last"]')
        .first()
        .simulate('click');
      expect(onSetPage.mock.calls).toHaveLength(0);
    });
  });

  describe('onFirst', () => {
    const onFirst = jest.fn();
    test('should call', () => {
      const wrapper = mount(<Pagination onFirstClick={onFirst} itemCount={40} page={2} />);
      wrapper
        .find('[data-action="first"]')
        .first()
        .simulate('click');
      expect(onFirst.mock.calls).toHaveLength(1);
      expect(onFirst.mock.calls[0][1]).toBe(1);
    });

    test('should NOT call', () => {
      const wrapper = mount(<Pagination itemCount={40} page={2} />);
      wrapper
        .find('[data-action="first"]')
        .first()
        .simulate('click');
      expect(onFirst.mock.calls).toHaveLength(0);
    });
  });

  describe('onLast', () => {
    const onLast = jest.fn();
    test('should call', () => {
      const wrapper = mount(<Pagination onLastClick={onLast} itemCount={40} />);
      wrapper
        .find('[data-action="last"]')
        .first()
        .simulate('click');
      expect(onLast.mock.calls).toHaveLength(1);
      expect(onLast.mock.calls[0][1]).toBe(4);
    });

    test('should NOT call', () => {
      const wrapper = mount(<Pagination itemCount={40} />);
      wrapper
        .find('[data-action="last"]')
        .first()
        .simulate('click');
      expect(onLast.mock.calls).toHaveLength(0);
    });
  });

  describe('onPrevious', () => {
    const onPrevious = jest.fn();
    test('should call', () => {
      const wrapper = mount(<Pagination onPreviousClick={onPrevious} itemCount={40} page={3} />);
      wrapper
        .find('[data-action="previous"]')
        .first()
        .simulate('click');
      expect(onPrevious.mock.calls).toHaveLength(1);
      expect(onPrevious.mock.calls[0][1]).toBe(2);
    });

    test('should NOT call', () => {
      const wrapper = mount(<Pagination itemCount={40} />);
      wrapper
        .find('[data-action="previous"]')
        .first()
        .simulate('click');
      expect(onPrevious.mock.calls).toHaveLength(0);
    });
  });

  describe('onNext', () => {
    const onNext = jest.fn();
    test('should call', () => {
      const wrapper = mount(<Pagination onNextClick={onNext} itemCount={40} />);
      wrapper
        .find('[data-action="next"]')
        .first()
        .simulate('click');
      expect(onNext.mock.calls).toHaveLength(1);
      expect(onNext.mock.calls[0][1]).toBe(2);
    });

    test('should NOT call', () => {
      const wrapper = mount(<Pagination itemCount={40} />);
      wrapper
        .find('[data-action="previous"]')
        .first()
        .simulate('click');
      expect(onNext.mock.calls).toHaveLength(0);
    });
  });

  describe('onPerPage', () => {
    const onPerPage = jest.fn();
    test('should call', () => {
      const wrapper = mount(<Pagination onPerPageSelect={onPerPage} itemCount={40} />);
      wrapper
        .find('.pf-c-options-menu button')
        .first()
        .simulate('click');
      wrapper.update();
      wrapper
        .find('ul li [data-action="per-page-20"]')
        .first()
        .simulate('click');
      expect(onPerPage.mock.calls).toHaveLength(1);
      expect(onPerPage.mock.calls[0][1]).toBe(20);
    });

    test('should NOT call', () => {
      const wrapper = mount(<Pagination itemCount={40} />);
      wrapper
        .find('.pf-c-options-menu button')
        .first()
        .simulate('click');
      wrapper.update();
      wrapper
        .find('ul li [data-action="per-page-20"]')
        .first()
        .simulate('click');
      expect(onPerPage.mock.calls).toHaveLength(0);
    });
  });
});
