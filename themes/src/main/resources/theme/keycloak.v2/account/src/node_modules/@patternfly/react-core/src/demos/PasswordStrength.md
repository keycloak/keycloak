---
id: Password strength
section: demos
---

import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';

## Demos

### Basic validation

Note, the validation and password strength rules are only examples, demonstrating the changes in the UI when certain conditions are met. We expect consumers will substitute their own, more robust, validation algorithm. In this demo the password strength is determined by how often validation rules are met. A good open-source password strength estimator, recommended by InfoSec, can be found here: https://github.com/dropbox/zxcvbn

```js
import React from 'react';
import {
  Form,
  FormGroup,
  FormHelperText,
  HelperText,
  Popover,
  HelperTextItem,
  TextInput
} from '@patternfly/react-core';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';

class PasswordStrengthDemo extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      password: '',
      ruleLength: 'indeterminate',
      ruleContent: 'indeterminate',
      ruleCharacters: 'indeterminate',
      passStrength: { variant: 'error', icon: <ExclamationCircleIcon />, text: 'Weak' }
    };

    this.handlePasswordInput = password => {
      this.setState({ password });
      this.validate(password);
    };

    this.validate = password => {
      if (password === '') {
        this.setState({
          ruleLength: 'indeterminate',
          ruleContent: 'indeterminate',
          ruleCharacters: 'indeterminate'
        });
        return;
      }

      if (password.length < 14) {
        this.setState({ ruleLength: 'error' });
      } else {
        this.setState({ ruleLength: 'success' });
      }

      if (/redhat/gi.test(password)) {
        this.setState({ ruleContent: 'error' });
      } else {
        this.setState({ ruleContent: 'success' });
      }

      let rulesCount = 0;
      let strCount = 0;
      if (/[a-z]/g.test(password)) {
        rulesCount++;
      }
      if (/[A-Z]/g.test(password)) {
        strCount += password.match(/[A-Z]/g).length;
        rulesCount++;
      }
      if (/\d/g.test(password)) {
        strCount += password.match(/\d/g).length;
        rulesCount++;
      }
      if (/\W/g.test(password)) {
        strCount += password.match(/\W/g).length;
        rulesCount++;
      }

      if (rulesCount < 3) {
        this.setState({ ruleCharacters: 'error' });
      } else {
        this.setState({ ruleCharacters: 'success' });
      }

      if (strCount < 3) {
        this.setState({ passStrength: { variant: 'error', icon: <ExclamationCircleIcon />, text: 'Weak' } });
      } else if (strCount < 5) {
        this.setState({ passStrength: { variant: 'warning', icon: <ExclamationTriangleIcon />, text: 'Medium' } });
      } else {
        this.setState({ passStrength: { variant: 'success', icon: <CheckCircleIcon />, text: 'Strong' } });
      }
    };
  }

  render() {
    const { password, ruleLength, ruleContent, ruleCharacters, passStrength } = this.state;

    const iconPopover = (
      <Popover headerContent={<div>Password Requirements</div>} bodyContent={<div>Password rules</div>}>
        <button
          type="button"
          aria-label="More info for name field"
          onClick={e => e.preventDefault()}
          aria-describedby="password-field"
          className="pf-c-form__group-label-help"
        >
          <HelpIcon noVerticalAlign />
        </button>
      </Popover>
    );

    let passStrLabel = (
      <HelperText>
        <HelperTextItem variant={passStrength.variant} icon={passStrength.icon}>
          {passStrength.text}
        </HelperTextItem>
      </HelperText>
    );

    return (
      <Form>
        <FormGroup
          label="Password"
          labelIcon={iconPopover}
          isRequired
          fieldId="password-field"
          {...(ruleLength === 'success' &&
            ruleContent === 'success' &&
            ruleCharacters === 'success' && {
              labelInfo: passStrLabel
            })}
        >
          <TextInput
            isRequired
            type="text"
            id="password-field"
            name="password-field"
            aria-describedby="password-field-helper"
            aria-invalid={ruleLength === 'error' || ruleContent === 'error' || ruleCharacters === 'error'}
            value={password}
            onChange={this.handlePasswordInput}
          />
          <FormHelperText isHidden={false} component="div">
            <HelperText component="ul" aria-live="polite" id="password-field-helper">
              <HelperTextItem isDynamic variant={ruleLength} component="li">
                Must be at least 14 characters
              </HelperTextItem>
              <HelperTextItem isDynamic variant={ruleContent} component="li">
                Cannot contain the word "redhat"
              </HelperTextItem>
              <HelperTextItem isDynamic variant={ruleCharacters} component="li">
                Must include at least 3 of the following: lowercase letter, uppercase letters, numbers, symbols
              </HelperTextItem>
            </HelperText>
          </FormHelperText>
        </FormGroup>
      </Form>
    );
  }
}
```
