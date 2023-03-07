import { Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Alert,
  Button,
  ButtonVariant,
  Divider,
  Form,
  FormGroup,
  Modal,
  Radio,
  Switch,
} from "@patternfly/react-core";

import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { HelpItem } from "ui-shared";

type ImportDialogProps = {
  onConfirm: (value: ResourceServerRepresentation) => void;
  closeDialog: () => void;
};

export const ImportDialog = ({ onConfirm, closeDialog }: ImportDialogProps) => {
  const { t } = useTranslation("clients");
  const [imported, setImported] = useState<ResourceServerRepresentation>({});
  return (
    <Modal
      title={t("import")}
      isOpen
      variant="small"
      onClose={closeDialog}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          onClick={() => {
            onConfirm(imported);
            closeDialog();
          }}
          data-testid="confirm"
        >
          {t("confirm")}
        </Button>,
        <Button
          data-testid="cancel"
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            closeDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form>
        <JsonFileUpload id="import-resource" onChange={setImported} />
      </Form>
      {Object.keys(imported).length !== 0 && (
        <>
          <Divider />
          <p className="pf-u-my-lg">{t("importResources")}</p>
          <Form isHorizontal>
            <FormGroup
              label={t("policyEnforcementMode")}
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:policyEnforcementMode")}
                  fieldLabelId="clients:policyEnforcementMode"
                />
              }
              fieldId="policyEnforcementMode"
              hasNoPaddingTop
            >
              <Radio
                id="policyEnforcementMode"
                name="policyEnforcementMode"
                label={t(
                  `policyEnforcementModes.${imported.policyEnforcementMode}`
                )}
                isChecked
                isDisabled
                className="pf-u-mb-md"
              />
            </FormGroup>
            <FormGroup
              label={t("decisionStrategy")}
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:decisionStrategy")}
                  fieldLabelId="clients:decisionStrategy"
                />
              }
              fieldId="decisionStrategy"
              hasNoPaddingTop
            >
              <Radio
                id="decisionStrategy"
                name="decisionStrategy"
                isChecked
                isDisabled
                label={t(`decisionStrategies.${imported.decisionStrategy}`)}
                className="pf-u-mb-md"
              />
            </FormGroup>
            <FormGroup
              hasNoPaddingTop
              label={t("allowRemoteResourceManagement")}
              fieldId="allowRemoteResourceManagement"
              labelIcon={
                <HelpItem
                  helpText={t("allowRemoteResourceManagement")}
                  fieldLabelId="clients:allowRemoteResourceManagement"
                />
              }
            >
              <Switch
                id="allowRemoteResourceManagement"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={imported.allowRemoteResourceManagement}
                isDisabled
                aria-label={t("allowRemoteResourceManagement")}
              />
            </FormGroup>
          </Form>
          <div className="pf-u-mt-md">
            {Object.entries(imported)
              .filter(([, value]) => Array.isArray(value))
              .map(([key, value]) => (
                <Fragment key={key}>
                  <Divider />
                  <p className="pf-u-my-sm">
                    <strong>
                      {value.length} {t(key)}
                    </strong>
                  </p>
                </Fragment>
              ))}
          </div>
          <Divider />
          <Alert
            variant="warning"
            className="pf-u-mt-lg"
            isInline
            title={t("importWarning")}
          />
        </>
      )}
    </Modal>
  );
};
