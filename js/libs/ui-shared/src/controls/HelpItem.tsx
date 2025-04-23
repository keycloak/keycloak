import { Icon, Popover } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { ReactNode } from "react";
import { useHelp } from "../context/HelpContext";
import style from "./helpitem.module.css";

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
            className={style.helpItem}
          >
            <Icon isInline={noVerticalAlign}>
              <HelpIcon />
            </Icon>
          </button>
        )}
        {unWrap && (
          <Icon isInline={noVerticalAlign}>
            <HelpIcon />
          </Icon>
        )}
      </>
    </Popover>
  ) : null;
};
