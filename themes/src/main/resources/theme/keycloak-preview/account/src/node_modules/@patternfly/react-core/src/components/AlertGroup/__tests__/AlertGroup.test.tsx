import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { Alert } from '../../Alert';
import { AlertGroup } from '../../AlertGroup';
import { AlertActionCloseButton } from '../../../components/Alert/AlertActionCloseButton';

jest.spyOn(document, 'createElement');
jest.spyOn(document.body, 'addEventListener');

test('Alert Group works with zero children', () => {
  const view = shallow(<AlertGroup></AlertGroup>);
  expect(view).toBeTruthy();
});

test('Alert Group should match snapshot', () => {
  const view = shallow(<AlertGroup></AlertGroup>);
  expect(view).toMatchSnapshot();
});

test('Alert Group works with n children', () => {
  const view = shallow(
    <AlertGroup>
      <Alert variant="success" title="alert title" />
      <Alert variant="warning" title="another alert title" />
    </AlertGroup>
  );
  expect(view).toBeTruthy();
});

test('Standard Alert Group is not a toast alert group', () => {
  const wrapper = mount(
    <AlertGroup>
      <Alert variant="danger" title="alert title" />
    </AlertGroup>
  );
  expect(wrapper.find('.pf-c-alert-group.pf-m-toast')).toHaveLength(0);
  expect(wrapper).toMatchSnapshot();
});

test('Toast Alert Group contains expected modifier class', () => {
  const wrapper = mount(
    <AlertGroup isToast>
      <Alert variant="warning" title="alert title" />
    </AlertGroup>
  );
  expect(wrapper.find('.pf-c-alert-group.pf-m-toast')).toHaveLength(1);
  expect(wrapper).toMatchSnapshot();
});

test('Alert Group creates a container element once for div', () => {
  const view = shallow(<AlertGroup> Test About Modal </AlertGroup>);
  view.update();
  expect(document.createElement).toBeCalledWith('div');
  expect(document.createElement).toHaveBeenCalledTimes(1);
});

test('alertgroup closes when alerts are closed', () => {
  const onClose = jest.fn();
  const wrapper = mount(
    <AlertGroup isToast appendTo={document.body}>
      <Alert
        isLiveRegion
        title={'Test Alert'}
        action={<AlertActionCloseButton aria-label="Close" onClose={onClose} />}
      />
    </AlertGroup>
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.find('button[aria-label="Close"]').simulate('click');
  expect(onClose).toBeCalled();
});
