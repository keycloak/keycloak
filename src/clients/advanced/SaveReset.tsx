import React from "react";
import { useTranslation } from "react-i18next";
import { ActionGroup, ActionGroupProps, Button } from "@patternfly/react-core";

type SaveResetProps = ActionGroupProps & {
  name: string;
  save: () => void;
  reset: () => void;
};

export const SaveReset = ({ name, save, reset, ...rest }: SaveResetProps) => {
  const { t } = useTranslation();
  return (
    <ActionGroup {...rest}>
      <Button data-testid={name + "Save"} variant="tertiary" onClick={save}>
        {t("common:save")}
      </Button>
      <Button data-testid={name + "Revert"} variant="link" onClick={reset}>
        {t("common:revert")}
      </Button>
    </ActionGroup>
  );
};
