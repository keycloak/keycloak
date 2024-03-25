import {
  FormHelperText,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";

export type FormErrorTextProps = {
  message: string;
};

export const FormErrorText = ({ message }: FormErrorTextProps) => {
  return (
    <FormHelperText>
      <HelperText>
        <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
          {message}
        </HelperTextItem>
      </HelperText>
    </FormHelperText>
  );
};
