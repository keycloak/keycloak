import * as React from 'react';
import { shallow } from 'enzyme';
import AddressBookIcon from '@patternfly/react-icons/dist/js/icons/address-book-icon';
import { EmptyState, EmptyStateVariant } from '../EmptyState';
import { EmptyStateBody } from '../EmptyStateBody';
import { EmptyStateSecondaryActions } from '../EmptyStateSecondaryActions';
import { EmptyStateIcon } from '../EmptyStateIcon';
import { EmptyStatePrimary } from '../EmptyStatePrimary';
import { Button } from '../../Button';
import { Title } from '../../Title';
import { BaseSizes } from '../../../styles/sizes';

describe('EmptyState', () => {
  test('Main', () => {
    const view = shallow(
      <EmptyState>
        <Title headingLevel="h5" size="lg">
          HTTP Proxies
        </Title>
        <EmptyStateBody>
          Defining HTTP Proxies that exist on your network allows you to perform various actions through those proxies.
        </EmptyStateBody>
        <Button variant="primary">New HTTP Proxy</Button>
        <EmptyStateSecondaryActions>
          <Button variant="link" aria-label="learn more action">
            Learn more about this in the documentation.
          </Button>
        </EmptyStateSecondaryActions>
      </EmptyState>
    );
    expect(view).toMatchSnapshot();
  });

  test('Main variant regular', () => {
    const view = shallow(
      <EmptyState variant={EmptyStateVariant.full}>
        <Title size={BaseSizes.md}>EmptyState full</Title>
      </EmptyState>
    );
    expect(view).toMatchSnapshot();
  });

  test('Main variant small', () => {
    const view = shallow(
      <EmptyState variant={EmptyStateVariant.small}>
        <Title size={BaseSizes.md}>EmptyState small</Title>
      </EmptyState>
    );
    expect(view).toMatchSnapshot();
  });

  test('Body', () => {
    const view = shallow(<EmptyStateBody className="custom-empty-state-body" id="empty-state-1" />);
    expect(view.props().className).toBe('pf-c-empty-state__body custom-empty-state-body');
    expect(view.props().id).toBe('empty-state-1');
  });

  test('Secondary Action', () => {
    const view = shallow(<EmptyStateSecondaryActions className="custom-empty-state-secondary" id="empty-state-2" />);
    expect(view.props().className).toBe('pf-c-empty-state__secondary custom-empty-state-secondary');
    expect(view.props().id).toBe('empty-state-2');
  });

  test('Icon', () => {
    const view = shallow(
      <EmptyStateIcon icon={AddressBookIcon} className="custom-empty-state-icon" id="empty-state-icon" />
    );
    expect(view.props().className).toBe('pf-c-empty-state__icon custom-empty-state-icon');
    expect(view.props().id).toBe('empty-state-icon');
  });

  test('Wrap icon in a div', () => {
    const view = shallow(
      <EmptyStateIcon
        variant="container"
        component={AddressBookIcon}
        className="custom-empty-state-icon"
        id="empty-state-icon"
      />
    );
    expect(view.find('div').props().className).toBe('pf-c-empty-state__icon custom-empty-state-icon');
    expect(view.find('AddressBookIcon').length).toBe(1);
  });

  test('Primary div', () => {
    const view = shallow(
      <EmptyStatePrimary className="custom-empty-state-prim-cls" id="empty-state-prim-id">
        <Button variant="link">Link</Button>
      </EmptyStatePrimary>
    );
    expect(view.props().className).toBe('pf-c-empty-state__primary custom-empty-state-prim-cls');
    expect(view.props().id).toBe('empty-state-prim-id');
  });
});
