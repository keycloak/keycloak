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

  return enabled ? (
    <Popover bodyContent={helpText}>
      <>
        {!unWrap && (
          <Button
            data-testid={`help-label-${fieldLabelId}`}
            aria-label={fieldLabelId}
            onClick={(e) => e.preventDefault()}
            variant="plain"
            className="pf-v6-c-form__group-label-help"
          >
            <Icon
              isInline={noVerticalAlign}
              status={isRecommendation ? "warning" : undefined}
            >
              <IconComponent />
            </Icon>
          </Button>
        )}
        {unWrap && (
          <Icon
            isInline={noVerticalAlign}
            status={isRecommendation ? "warning" : undefined}
          >
            <IconComponent />
          </Icon>
        )}
      </>
    </Popover>
  ) : null;
};
