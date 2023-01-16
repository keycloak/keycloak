import { useTranslation } from "react-i18next";
import { ActionGroup, ActionGroupProps, Button } from "@patternfly/react-core";

type SaveResetProps = ActionGroupProps & {
  name: string;
  save?: () => void;
  reset: () => void;
  isActive?: boolean;
};

export const SaveReset = ({
  name,
  save,
  reset,
  isActive = true,
  ...rest
}: SaveResetProps) => {
  const { t } = useTranslation("common");
  return (
    <ActionGroup {...rest}>
      <Button
        isDisabled={!isActive}
        data-testid={name + "Save"}
        onClick={save}
        type={save ? "button" : "submit"}
      >
        {t("save")}
      </Button>
      <Button
        isDisabled={!isActive}
        data-testid={name + "Revert"}
        variant="link"
        onClick={reset}
      >
        {t("revert")}
      </Button>
    </ActionGroup>
  );
};
