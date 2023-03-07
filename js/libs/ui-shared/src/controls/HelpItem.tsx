import { Popover } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { ReactNode } from "react";
import { useHelp } from "../context/HelpContext";

type HelpItemProps = {
  helpText: string | ReactNode;
  fieldLabelId: string;
  noVerticalAlign?: boolean;
  unWrap?: boolean;
};

export const HelpItem = ({
  helpText,
  fieldLabelId,
  noVerticalAlign = true,
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
            <HelpIcon noVerticalAlign={noVerticalAlign} />
          </button>
        )}
        {unWrap && <HelpIcon noVerticalAlign={noVerticalAlign} />}
      </>
    </Popover>
  ) : null;
};
