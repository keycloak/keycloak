import * as React from 'react';
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
export declare const LoginForm: React.FunctionComponent<LoginFormProps>;
//# sourceMappingURL=LoginForm.d.ts.map