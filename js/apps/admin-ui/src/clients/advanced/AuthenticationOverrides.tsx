import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { sortBy } from "lodash-es";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

type AuthenticationOverridesProps = {
  save: () => void;
  reset: () => void;
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const AuthenticationOverrides = ({
  protocol,
  save,
  reset,
  hasConfigureAccess,
}: AuthenticationOverridesProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation("clients");
  const [flows, setFlows] = useState<JSX.Element[]>([]);
  const [browserFlowOpen, setBrowserFlowOpen] = useState(false);
  const [directGrantOpen, setDirectGrantOpen] = useState(false);

  const { control } = useFormContext();

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) => {
      let filteredFlows = [
        ...flows.filter((flow) => flow.providerId !== "client-flow"),
      ];
      filteredFlows = sortBy(filteredFlows, [(f) => f.alias]);
      setFlows([
        <SelectOption key="empty" value="">
          {t("common:choose")}
        </SelectOption>,
        ...filteredFlows.map((flow) => (
          <SelectOption key={flow.id} value={flow.id}>
            {flow.alias}
          </SelectOption>
        )),
      ]);
    },
    []
  );

  return (
    <FormAccess
      role="manage-clients"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      <FormGroup
        label={t("browserFlow")}
        fieldId="browserFlow"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:browserFlow")}
            fieldLabelId="clients:browserFlow"
          />
        }
      >
        <Controller
          name="authenticationFlowBindingOverrides.browser"
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="browserFlow"
              variant={SelectVariant.single}
              onToggle={setBrowserFlowOpen}
              isOpen={browserFlowOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setBrowserFlowOpen(false);
              }}
              selections={[field.value]}
            >
              {flows}
            </Select>
          )}
        />
      </FormGroup>
      {protocol === "openid-connect" && (
        <FormGroup
          label={t("directGrant")}
          fieldId="directGrant"
          labelIcon={
            <HelpItem
              helpText={t("clients-help:directGrant")}
              fieldLabelId="clients:directGrant"
            />
          }
        >
          <Controller
            name="authenticationFlowBindingOverrides.direct_grant"
            defaultValue=""
            control={control}
            render={({ field }) => (
              <Select
                toggleId="directGrant"
                variant={SelectVariant.single}
                onToggle={setDirectGrantOpen}
                isOpen={directGrantOpen}
                onSelect={(_, value) => {
                  field.onChange(value);
                  setDirectGrantOpen(false);
                }}
                selections={[field.value]}
              >
                {flows}
              </Select>
            )}
          />
        </FormGroup>
      )}
      <ActionGroup>
        <Button
          variant="secondary"
          onClick={save}
          data-testid="OIDCAuthFlowOverrideSave"
        >
          {t("common:save")}
        </Button>
        <Button
          variant="link"
          onClick={reset}
          data-testid="OIDCAuthFlowOverrideRevert"
        >
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
