import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  Radio,
} from "@patternfly/react-core";
import {
  AllClientScopes,
  AllClientScopeType,
  allClientScopeTypes,
} from "../components/client-scope/ClientScopeTypes";

type ChangeTypeDialogProps = {
  selectedClientScopes: number;
  onConfirm: (scope: AllClientScopeType) => void;
  onClose: () => void;
};

export const ChangeTypeDialog = ({
  selectedClientScopes,
  onConfirm,
  onClose,
}: ChangeTypeDialogProps) => {
  const { t } = useTranslation("client-scopes");
  const [value, setValue] = useState<AllClientScopeType>(AllClientScopes.none);
  return (
    <Modal
      title={t("changeType")}
      isOpen={true}
      onClose={onClose}
      variant="small"
      description={t("changeTypeIntro", { count: selectedClientScopes })}
      actions={[
        <Button
          data-testid="change-scope-dialog-confirm"
          key="confirm"
          onClick={() => onConfirm(value)}
        >
          {t("common:continue")}
        </Button>,
        <Button
          key="cancel"
          variant={ButtonVariant.secondary}
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form isHorizontal>
        {allClientScopeTypes.map((scope) => (
          <Radio
            key={scope}
            isChecked={scope === value}
            name={`radio-${scope}`}
            onChange={(_val, event) => {
              const { value } = event.currentTarget;
              setValue(value as AllClientScopeType);
            }}
            label={t(`common:clientScope.${scope}`)}
            id={`radio-${scope}`}
            value={scope}
          />
        ))}
      </Form>
    </Modal>
  );
};
