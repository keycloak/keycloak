import {
  FormHelperText,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";

export type FormValidationMessageProps = {
  message: string;
};

export const FormValidationMessage = ({
  message,
}: FormValidationMessageProps) => (
  <FormHelperText>
    <HelperText>
      <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
        {message}
      </HelperTextItem>
    </HelperText>
  </FormHelperText>
);
