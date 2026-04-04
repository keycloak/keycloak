import { Icon, Popover } from "@patternfly/react-core";
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
          <button
            data-testid={`help-label-${fieldLabelId}`}
            aria-label={fieldLabelId}
            onClick={(e) => e.preventDefault()}
            className="pf-v5-c-form__group-label-help"
          >
            <Icon
              isInline={noVerticalAlign}
              status={isRecommendation ? "warning" : undefined}
            >
              <IconComponent />
            </Icon>
          </button>
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
