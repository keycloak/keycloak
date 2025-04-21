"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginForm = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Form_1 = require("../Form");
const TextInput_1 = require("../TextInput");
const Button_1 = require("../Button");
const Checkbox_1 = require("../Checkbox");
const constants_1 = require("../../helpers/constants");
const InputGroup_1 = require("../InputGroup");
const eye_slash_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/eye-slash-icon'));
const eye_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/eye-icon'));
const LoginForm = (_a) => {
    var { noAutoFocus = false, className = '', showHelperText = false, helperText = null, helperTextIcon = null, usernameLabel = 'Username', usernameValue = '', onChangeUsername = () => undefined, isValidUsername = true, passwordLabel = 'Password', passwordValue = '', onChangePassword = () => undefined, isShowPasswordEnabled = false, hidePasswordAriaLabel = 'Hide password', showPasswordAriaLabel = 'Show password', isValidPassword = true, loginButtonLabel = 'Log In', isLoginButtonDisabled = false, onLoginButtonClick = () => undefined, rememberMeLabel = '', isRememberMeChecked = false, onChangeRememberMe = () => undefined } = _a, props = tslib_1.__rest(_a, ["noAutoFocus", "className", "showHelperText", "helperText", "helperTextIcon", "usernameLabel", "usernameValue", "onChangeUsername", "isValidUsername", "passwordLabel", "passwordValue", "onChangePassword", "isShowPasswordEnabled", "hidePasswordAriaLabel", "showPasswordAriaLabel", "isValidPassword", "loginButtonLabel", "isLoginButtonDisabled", "onLoginButtonClick", "rememberMeLabel", "isRememberMeChecked", "onChangeRememberMe"]);
    const [passwordHidden, setPasswordHidden] = React.useState(true);
    const passwordInput = (React.createElement(TextInput_1.TextInput, { isRequired: true, type: passwordHidden ? 'password' : 'text', id: "pf-login-password-id", name: "pf-login-password-id", validated: isValidPassword ? constants_1.ValidatedOptions.default : constants_1.ValidatedOptions.error, value: passwordValue, onChange: onChangePassword }));
    return (React.createElement(Form_1.Form, Object.assign({ className: className }, props),
        React.createElement(Form_1.FormHelperText, { isError: !isValidUsername || !isValidPassword, isHidden: !showHelperText, icon: helperTextIcon }, helperText),
        React.createElement(Form_1.FormGroup, { label: usernameLabel, isRequired: true, validated: isValidUsername ? constants_1.ValidatedOptions.default : constants_1.ValidatedOptions.error, fieldId: "pf-login-username-id" },
            React.createElement(TextInput_1.TextInput, { autoFocus: !noAutoFocus, id: "pf-login-username-id", isRequired: true, validated: isValidUsername ? constants_1.ValidatedOptions.default : constants_1.ValidatedOptions.error, type: "text", name: "pf-login-username-id", value: usernameValue, onChange: onChangeUsername })),
        React.createElement(Form_1.FormGroup, { label: passwordLabel, isRequired: true, validated: isValidPassword ? constants_1.ValidatedOptions.default : constants_1.ValidatedOptions.error, fieldId: "pf-login-password-id" },
            isShowPasswordEnabled && (React.createElement(InputGroup_1.InputGroup, null,
                passwordInput,
                React.createElement(Button_1.Button, { variant: "control", onClick: () => setPasswordHidden(!passwordHidden), "aria-label": passwordHidden ? showPasswordAriaLabel : hidePasswordAriaLabel }, passwordHidden ? React.createElement(eye_icon_1.default, null) : React.createElement(eye_slash_icon_1.default, null)))),
            !isShowPasswordEnabled && passwordInput),
        rememberMeLabel.length > 0 && (React.createElement(Form_1.FormGroup, { fieldId: "pf-login-remember-me-id" },
            React.createElement(Checkbox_1.Checkbox, { id: "pf-login-remember-me-id", label: rememberMeLabel, isChecked: isRememberMeChecked, onChange: onChangeRememberMe }))),
        React.createElement(Form_1.ActionGroup, null,
            React.createElement(Button_1.Button, { variant: "primary", type: "submit", onClick: onLoginButtonClick, isBlock: true, isDisabled: isLoginButtonDisabled }, loginButtonLabel))));
};
exports.LoginForm = LoginForm;
exports.LoginForm.displayName = 'LoginForm';
//# sourceMappingURL=LoginForm.js.map