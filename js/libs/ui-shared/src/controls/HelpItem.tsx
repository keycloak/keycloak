import { Icon, Popover } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { ReactNode } from "react";
import { useHelp } from "../context/HelpContext";

type HelpItemProps = {
  helpText: string | ReactNode;
  fieldLabelId: string;
  isInline?: boolean;
  unWrap?: boolean;
};

export const HelpItem = ({
  helpText,
  fieldLabelId,
  isInline = false,
  unWrap = false,
}: HelpItemProps) => {
  const { enabled } = useHelp();
  return enabled ? (
    <Popover bodyContent={helpText}>
      <>
        {!unWrap && (
          <button
            data-testid={`help-label-${fieldLabelId}`}
            aria-label={fieldLabelId}
            onClick={(e) => e.preventDefault()}
            className="pf-c-form__group-label-help"
          >
            <Icon isInline={isInline}>
              <HelpIcon />
            </Icon>
          </button>
        )}
        {unWrap && (
          <Icon isInline={isInline}>
            <HelpIcon />
          </Icon>
        )}
      </>
    </Popover>
  ) : null;
};
