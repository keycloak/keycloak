import { Popover } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { isValidElement, ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { useHelp } from "ui-shared";

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
  const { t } = useTranslation();
  const { enabled } = useHelp();
  return enabled ? (
    <Popover
      bodyContent={isValidElement(helpText) ? helpText : t(helpText as string)}
    >
      <>
        {!unWrap && (
          <button
            data-testid={`help-label-${t(fieldLabelId)
              .toLowerCase()
              .replace(/\s/g, "-")}`}
            aria-label={t("helpLabel", { label: t(fieldLabelId) })}
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
