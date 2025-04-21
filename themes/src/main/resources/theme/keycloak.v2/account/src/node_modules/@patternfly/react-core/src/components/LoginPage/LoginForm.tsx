import * as React from 'react';
import { Form, FormGroup, ActionGroup, FormHelperText } from '../Form';
import { TextInput } from '../TextInput';
import { Button } from '../Button';
import { Checkbox } from '../Checkbox';
import { ValidatedOptions } from '../../helpers/constants';
import { InputGroup } from '../InputGroup';
import EyeSlashIcon from '@patternfly/react-icons/dist/esm/icons/eye-slash-icon';
import EyeIcon from '@patternfly/react-icons/dist/esm/icons/eye-icon';

export interface LoginFormProps extends React.HTMLProps<HTMLFormElement> {
  /** Flag to indicate if the first dropdown item should not gain initial focus */
  noAutoFocus?: boolean;
  /** Additional classes added to the Login Main Body's Form */
  className?: string;
  /** Flag indicating the Helper Text is visible * */
  showHelperText?: boolean;
  /** Content displayed in the Helper Text component * */
  helperText?: React.ReactNode;
  /** Icon displayed to the left in the Helper Text */
  helperTextIcon?: React.ReactNode;
  /** Label for the Username Input Field */
  usernameLabel?: string;
  /** Value for the Username */
  usernameValue?: string;
  /** Function that handles the onChange event for the Username */
  onChangeUsername?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
  /** Flag indicating if the Username is valid */
  isValidUsername?: boolean;
  /** Label for the Password Input Field */
  passwordLabel?: string;
  /** Value for the Password */
  passwordValue?: string;
  /** Function that handles the onChange event for the Password */
  onChangePassword?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
  /** Flag indicating if the Password is valid */
  isValidPassword?: boolean;
  /** Flag indicating if the user can toggle hiding the password */
  isShowPasswordEnabled?: boolean;
  /** Accessible label for the show password button */
  showPasswordAriaLabel?: string;
  /** Accessible label for the hide password button */
  hidePasswordAriaLabel?: string;
  /** Label for the Log in Button Input */
  loginButtonLabel?: string;
  /** Flag indicating if the Login Button is disabled */
  isLoginButtonDisabled?: boolean;
  /** Function that is called when the Login button is clicked */
  onLoginButtonClick?: (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
  /** Label for the Remember Me Checkbox that indicates the user should be kept logged in.  If the label is not provided, the checkbox will not show. */
  rememberMeLabel?: string;
  /** Flag indicating if the remember me Checkbox is checked. */
  isRememberMeChecked?: boolean;
  /** Function that handles the onChange event for the Remember Me Checkbox */
  onChangeRememberMe?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
}

export const LoginForm: React.FunctionComponent<LoginFormProps> = ({
  noAutoFocus = false,
  className = '',
  showHelperText = false,
  helperText = null,
  helperTextIcon = null,
  usernameLabel = 'Username',
  usernameValue = '',
  onChangeUsername = () => undefined as any,
  isValidUsername = true,
  passwordLabel = 'Password',
  passwordValue = '',
  onChangePassword = () => undefined as any,
  isShowPasswordEnabled = false,
  hidePasswordAriaLabel = 'Hide password',
  showPasswordAriaLabel = 'Show password',
  isValidPassword = true,
  loginButtonLabel = 'Log In',
  isLoginButtonDisabled = false,
  onLoginButtonClick = () => undefined as any,
  rememberMeLabel = '',
  isRememberMeChecked = false,
  onChangeRememberMe = () => undefined as any,
  ...props
}: LoginFormProps) => {
  const [passwordHidden, setPasswordHidden] = React.useState(true);

  const passwordInput = (
    <TextInput
      isRequired
      type={passwordHidden ? 'password' : 'text'}
      id="pf-login-password-id"
      name="pf-login-password-id"
      validated={isValidPassword ? ValidatedOptions.default : ValidatedOptions.error}
      value={passwordValue}
      onChange={onChangePassword}
    />
  );

  return (
    <Form className={className} {...props}>
      <FormHelperText isError={!isValidUsername || !isValidPassword} isHidden={!showHelperText} icon={helperTextIcon}>
        {helperText}
      </FormHelperText>
      <FormGroup
        label={usernameLabel}
        isRequired
        validated={isValidUsername ? ValidatedOptions.default : ValidatedOptions.error}
        fieldId="pf-login-username-id"
      >
        <TextInput
          autoFocus={!noAutoFocus}
          id="pf-login-username-id"
          isRequired
          validated={isValidUsername ? ValidatedOptions.default : ValidatedOptions.error}
          type="text"
          name="pf-login-username-id"
          value={usernameValue}
          onChange={onChangeUsername}
        />
      </FormGroup>
      <FormGroup
        label={passwordLabel}
        isRequired
        validated={isValidPassword ? ValidatedOptions.default : ValidatedOptions.error}
        fieldId="pf-login-password-id"
      >
        {isShowPasswordEnabled && (
          <InputGroup>
            {passwordInput}
            <Button
              variant="control"
              onClick={() => setPasswordHidden(!passwordHidden)}
              aria-label={passwordHidden ? showPasswordAriaLabel : hidePasswordAriaLabel}
            >
              {passwordHidden ? <EyeIcon /> : <EyeSlashIcon />}
            </Button>
          </InputGroup>
        )}
        {!isShowPasswordEnabled && passwordInput}
      </FormGroup>
      {rememberMeLabel.length > 0 && (
        <FormGroup fieldId="pf-login-remember-me-id">
          <Checkbox
            id="pf-login-remember-me-id"
            label={rememberMeLabel}
            isChecked={isRememberMeChecked}
            onChange={onChangeRememberMe}
          />
        </FormGroup>
      )}
      <ActionGroup>
        <Button variant="primary" type="submit" onClick={onLoginButtonClick} isBlock isDisabled={isLoginButtonDisabled}>
          {loginButtonLabel}
        </Button>
      </ActionGroup>
    </Form>
  );
};
LoginForm.displayName = 'LoginForm';
