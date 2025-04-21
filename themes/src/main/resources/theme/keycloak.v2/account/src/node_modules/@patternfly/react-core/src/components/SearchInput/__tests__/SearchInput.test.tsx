import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SearchInput } from '../SearchInput';
import { FormGroup } from '../../Form';
import { Button } from '../../Button';
import { ExternalLinkSquareAltIcon } from '@patternfly/react-icons';

const props = {
  onChange: jest.fn(),
  value: 'test input',
  onNextClick: jest.fn(),
  onPreviousClick: jest.fn(),
  onClear: jest.fn(),
  onSearch: jest.fn()
};

describe('SearchInput', () => {
  test('simple search input', () => {
    const { asFragment } = render(<SearchInput {...props} aria-label="simple text input" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('search input with hint', () => {
    const { asFragment } = render(<SearchInput {...props} hint="test hint" aria-label="simple text input" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('result count', () => {
    render(<SearchInput {...props} resultsCount={3} aria-label="simple text input" data-testid="test-id" />);
    expect(screen.getByTestId('test-id').querySelector('.pf-c-badge')).toBeInTheDocument();
  });

  test('navigable search results', () => {
    render(<SearchInput {...props} resultsCount="3 / 7" aria-label="simple text input" data-testid="test-id" />);

    const input = screen.getByTestId('test-id');
    expect(input.querySelector('.pf-c-text-input-group__group')).toBeInTheDocument();
    expect(input.querySelector('.pf-c-badge')).toBeInTheDocument();

    userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    expect(props.onPreviousClick).toHaveBeenCalled();

    userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(props.onNextClick).toHaveBeenCalled();

    userEvent.click(screen.getByRole('button', { name: 'Reset' }));
    expect(props.onClear).toHaveBeenCalled();
  });

  test('hide clear button', () => {
    const { onClear, ...testProps } = props;

    render(<SearchInput {...testProps} resultsCount="3" aria-label="simple text input without on clear" />);
    expect(screen.queryByRole('button', { name: 'Reset' })).not.toBeInTheDocument();
  });

  test('advanced search', () => {
    const { asFragment } = render(
      <SearchInput
        attributes={[
          { attr: 'username', display: 'Username' },
          { attr: 'firstname', display: 'First name' }
        ]}
        advancedSearchDelimiter=":"
        value="username:player firstname:john"
        onChange={props.onChange}
        onSearch={props.onSearch}
        onClear={props.onClear}
      />
    );

    userEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(props.onSearch).toHaveBeenCalled();
    expect(asFragment()).toMatchSnapshot();
  });

  test('advanced search with custom attributes', () => {
    const { asFragment } = render(
      <SearchInput
        attributes={[
          { attr: 'username', display: 'Username' },
          { attr: 'firstname', display: 'First name' }
        ]}
        advancedSearchDelimiter=":"
        formAdditionalItems={
          <FormGroup fieldId="test-form-group">
            <Button variant="link" isInline icon={<ExternalLinkSquareAltIcon />} iconPosition="right">
              Link
            </Button>
          </FormGroup>
        }
        value="username:player firstname:john"
        onChange={props.onChange}
        onSearch={props.onSearch}
        onClear={props.onClear}
      />
    );

    userEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(props.onSearch).toHaveBeenCalled();
    expect(asFragment()).toMatchSnapshot();
  });
});
