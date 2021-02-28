import React from "react";
import { useTranslation } from "react-i18next";
import { ActionGroup, Button } from "@patternfly/react-core";

type SaveResetProps = {
  name: string;
  save: () => void;
  reset: () => void;
};

export const SaveReset = ({ name, save, reset }: SaveResetProps) => {
  const { t } = useTranslation();
  return (
    <ActionGroup>
      <Button data-testid={name + "Save"} variant="tertiary" onClick={save}>
        {t("common:save")}
      </Button>
      <Button data-testid={name + "Reload"} variant="link" onClick={reset}>
        {t("common:reload")}
      </Button>
    </ActionGroup>
  );
};
