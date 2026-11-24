import { Button, Icon, Popover } from "@patternfly/react-core";
import { HelpIcon, ExclamationTriangleIcon } from "@patternfly/react-icons";
import { ReactNode } from "react";
import { useHelp } from "../context/HelpContext";

type HelpItemProps = {
  helpText: string | ReactNode;
  fieldLabelId: string;
  noVerticalAlign?: boolean;
  unWrap?: boolean;
  isRecommendation?: boolean;
};

export const HelpItem = ({
  helpText,
  fieldLabelId,
  noVerticalAlign = true,
  unWrap = false,
  isRecommendation = false,
}: HelpItemProps) => {
  const { enabled } = useHelp();
  const IconComponent = isRecommendation ? ExclamationTriangleIcon : HelpIcon;

  if (!enabled) {
    return null;
  }

  // PF6 Popover requires a single element child that can take a ref — no Fragment.
  if (unWrap) {
    return (
      <Popover bodyContent={helpText}>
        <Icon
          isInline={noVerticalAlign}
          status={isRecommendation ? "warning" : undefined}
        >
          <IconComponent />
        </Icon>
      </Popover>
    );
  }

  return (
    <Popover bodyContent={helpText}>
      <Button
        variant="plain"
        data-testid={`help-label-${fieldLabelId}`}
        aria-label={fieldLabelId}
        onClick={(e) => e.preventDefault()}
        className="pf-v6-c-form__group-label-help"
        icon={
          <Icon
            isInline={noVerticalAlign}
            status={isRecommendation ? "warning" : undefined}
          >
            <IconComponent />
          </Icon>
        }
      />
    </Popover>
  );
};
