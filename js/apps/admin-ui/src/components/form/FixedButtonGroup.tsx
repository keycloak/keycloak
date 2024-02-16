import { useTranslation } from "react-i18next";
import { ActionGroup, ActionGroupProps, Button } from "@patternfly/react-core";
import { PropsWithChildren } from "react";

import style from "./fixed-buttons.module.css";

type FixedButtonGroupProps = ActionGroupProps & {
  name: string;
  save?: () => void;
  reset?: () => void;
  isSubmit?: boolean;
  isActive?: boolean;
};

export const FixedButtonsGroup = ({
  name,
  save,
  reset,
  isSubmit = false,
  isActive = true,
  children,
  ...rest
}: PropsWithChildren<FixedButtonGroupProps>) => {
  const { t } = useTranslation();
  return (
    <ActionGroup className={style.buttonGroup} {...rest}>
      {(save || isSubmit) && (
        <Button
          isDisabled={!isActive}
          data-testid={`${name}-save`}
          onClick={() => save?.()}
          type={isSubmit ? "submit" : "button"}
        >
          {t("save")}
        </Button>
      )}
      {reset && (
        <Button
          isDisabled={!isActive}
          data-testid={`${name}-revert`}
          variant="link"
          onClick={() => reset()}
        >
          {t("revert")}
        </Button>
      )}
      {children}
    </ActionGroup>
  );
};
