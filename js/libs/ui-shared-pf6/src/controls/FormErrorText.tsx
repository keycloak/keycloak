import {
  FormHelperText,
  FormHelperTextProps,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";

export type FormErrorTextProps = FormHelperTextProps & {
  message: string;
};

export const FormErrorText = ({ message, ...props }: FormErrorTextProps) => {
  return (
    <FormHelperText {...props}>
      <HelperText>
        <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
          {message}
        </HelperTextItem>
      </HelperText>
    </FormHelperText>
  );
};
