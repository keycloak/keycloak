import { Label, Tooltip } from "@patternfly/react-core";
import { WarningTriangleIcon } from "@patternfly/react-icons";

type MissingPolicyReferenceProps = {
  id?: string;
  label: string;
};

export const MissingPolicyReference = ({
  id,
  label,
}: MissingPolicyReferenceProps) => (
  <Tooltip content={id}>
    <Label color="orange" icon={<WarningTriangleIcon />} tabIndex={0}>
      {label}
    </Label>
  </Tooltip>
);
