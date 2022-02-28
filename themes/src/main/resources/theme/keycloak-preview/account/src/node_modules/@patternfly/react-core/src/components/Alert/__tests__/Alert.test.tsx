import * as React from 'react';
import { mount } from 'enzyme';

import { Alert, AlertVariant } from '../Alert';
import { AlertActionLink } from '../AlertActionLink';
import { AlertActionCloseButton } from '../AlertActionCloseButton';

test('default Alert variant is info', () => {
  const view = mount(<Alert title="this is a test">Alert testing</Alert>);
  expect(
    view
      .find('Alert')
      .childAt(0)
      .prop('className')
  ).toContain('pf-m-info');
});

Object.values(AlertVariant).forEach(variant => {
  describe(`Alert - ${variant}`, () => {
    test('Description', () => {
      const view = mount(
        <Alert variant={variant} title="">
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('Title', () => {
      const view = mount(
        <Alert variant={variant} title="Some title">
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('Action Link', () => {
      const view = mount(
        <Alert variant={variant} action={<AlertActionLink>test</AlertActionLink>} title="">
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('Action Close Button', () => {
      const onClose = jest.fn();
      const view = mount(
        <Alert
          variant={variant}
          action={<AlertActionCloseButton aria-label="Close" onClose={onClose} />}
          title={`Sample ${variant} alert`}
        >
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
      view.find('button[aria-label="Close"]').simulate('click');
      expect(onClose).toHaveBeenCalled();
    });

    test('Action and Title', () => {
      const view = mount(
        <Alert variant={variant} action={<AlertActionLink>test</AlertActionLink>} title="Some title">
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('Custom aria label', () => {
      const view = mount(
        <Alert
          variant={variant}
          aria-label={`Custom aria label for ${variant}`}
          action={<AlertActionLink>test</AlertActionLink>}
          title="Some title"
        >
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('inline variation', () => {
      const view = mount(
        <Alert variant={variant} isInline title="Some title">
          Some alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('Toast alerts match snapsnot', () => {
      const view = mount(
        <Alert isLiveRegion={true} variant={variant} aria-label={`${variant} toast alert`} title="Some title">
          Some toast alert
        </Alert>
      );
      expect(view).toMatchSnapshot();
    });

    test('Toast alerts contain default live region', () => {
      const wrapper = mount(
        <Alert isLiveRegion={true} variant={variant} aria-label={`${variant} toast alert`} title="Some title">
          Some toast alert
        </Alert>
      );
      const liveRegion = wrapper.find({ 'aria-live': 'polite' }).length;
      expect(liveRegion).toBe(1);
    });

    test('Toast alert live regions are not atomic', () => {
      const wrapper = mount(
        <Alert isLiveRegion={true} variant={variant} aria-label={`${variant} toast alert`} title="Some title">
          Some toast alert
        </Alert>
      );
      expect(wrapper.find('.pf-c-alert').prop('aria-atomic')).toBe('false');
    });

    test('Non-toast alerts can have custom live region settings', () => {
      const wrapper = mount(
        <Alert
          aria-live="assertive"
          aria-relevant="all"
          aria-atomic="true"
          variant={variant}
          aria-label={`${variant} toast alert`}
          title="Some title"
        >
          Some noisy alert
        </Alert>
      );
      const alert = wrapper.find(Alert);

      expect(alert.prop('aria-live')).toBe('assertive');
      expect(alert.prop('aria-relevant')).toBe('all');
      expect(alert.prop('aria-atomic')).toBe('true');
    });
  });
});
