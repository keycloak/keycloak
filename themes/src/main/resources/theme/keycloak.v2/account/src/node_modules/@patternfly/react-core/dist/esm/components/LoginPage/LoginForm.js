import { __rest } from "tslib";
import * as React from 'react';
import { Form, FormGroup, ActionGroup, FormHelperText } from '../Form';
import { TextInput } from '../TextInput';
import { Button } from '../Button';
import { Checkbox } from '../Checkbox';
import { ValidatedOptions } from '../../helpers/constants';
import { InputGroup } from '../InputGroup';
import EyeSlashIcon from '@patternfly/react-icons/dist/esm/icons/eye-slash-icon';
import EyeIcon from '@patternfly/react-icons/dist/esm/icons/eye-icon';
export const LoginForm = (_a) => {
    var { noAutoFocus = false, className = '', showHelperText = false, helperText = null, helperTextIcon = null, usernameLabel = 'Username', usernameValue = '', onChangeUsername = () => undefined, isValidUsername = true, passwordLabel = 'Password', passwordValue = '', onChangePassword = () => undefined, isShowPasswordEnabled = false, hidePasswordAriaLabel = 'Hide password', showPasswordAriaLabel = 'Show password', isValidPassword = true, loginButtonLabel = 'Log In', isLoginButtonDisabled = false, onLoginButtonClick = () => undefined, rememberMeLabel = '', isRememberMeChecked = false, onChangeRememberMe = () => undefined } = _a, props = __rest(_a, ["noAutoFocus", "className", "showHelperText", "helperText", "helperTextIcon", "usernameLabel", "usernameValue", "onChangeUsername", "isValidUsername", "passwordLabel", "passwordValue", "onChangePassword", "isShowPasswordEnabled", "hidePasswordAriaLabel", "showPasswordAriaLabel", "isValidPassword", "loginButtonLabel", "isLoginButtonDisabled", "onLoginButtonClick", "rememberMeLabel", "isRememberMeChecked", "onChangeRememberMe"]);
    const [passwordHidden, setPasswordHidden] = React.useState(true);
    const passwordInput = (React.createElement(TextInput, { isRequired: true, type: passwordHidden ? 'password' : 'text', id: "pf-login-password-id", name: "pf-login-password-id", validated: isValidPassword ? ValidatedOptions.default : ValidatedOptions.error, value: passwordValue, onChange: onChangePassword }));
    return (React.createElement(Form, Object.assign({ className: className }, props),
        React.createElement(FormHelperText, { isError: !isValidUsername || !isValidPassword, isHidden: !showHelperText, icon: helperTextIcon }, helperText),
        React.createElement(FormGroup, { label: usernameLabel, isRequired: true, validated: isValidUsername ? ValidatedOptions.default : ValidatedOptions.error, fieldId: "pf-login-username-id" },
            React.createElement(TextInput, { autoFocus: !noAutoFocus, id: "pf-login-username-id", isRequired: true, validated: isValidUsername ? ValidatedOptions.default : ValidatedOptions.error, type: "text", name: "pf-login-username-id", value: usernameValue, onChange: onChangeUsername })),
        React.createElement(FormGroup, { label: passwordLabel, isRequired: true, validated: isValidPassword ? ValidatedOptions.default : ValidatedOptions.error, fieldId: "pf-login-password-id" },
            isShowPasswordEnabled && (React.createElement(InputGroup, null,
                passwordInput,
                React.createElement(Button, { variant: "control", onClick: () => setPasswordHidden(!passwordHidden), "aria-label": passwordHidden ? showPasswordAriaLabel : hidePasswordAriaLabel }, passwordHidden ? React.createElement(EyeIcon, null) : React.createElement(EyeSlashIcon, null)))),
            !isShowPasswordEnabled && passwordInput),
        rememberMeLabel.length > 0 && (React.createElement(FormGroup, { fieldId: "pf-login-remember-me-id" },
            React.createElement(Checkbox, { id: "pf-login-remember-me-id", label: rememberMeLabel, isChecked: isRememberMeChecked, onChange: onChangeRememberMe }))),
        React.createElement(ActionGroup, null,
            React.createElement(Button, { variant: "primary", type: "submit", onClick: onLoginButtonClick, isBlock: true, isDisabled: isLoginButtonDisabled }, loginButtonLabel))));
};
LoginForm.displayName = 'LoginForm';
//# sourceMappingURL=LoginForm.js.map