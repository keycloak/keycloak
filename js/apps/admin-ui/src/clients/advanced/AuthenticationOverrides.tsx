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

import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";

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
  const { t } = useTranslation();
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
          {t("choose")}
        </SelectOption>,
        ...filteredFlows.map((flow) => (
          <SelectOption key={flow.id} value={flow.id}>
            {flow.alias}
          </SelectOption>
        )),
      ]);
    },
    [],
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
            helpText={t("browserFlowHelp")}
            fieldLabelId="browserFlow"
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
              helpText={t("directGrantHelp")}
              fieldLabelId="directGrant"
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
          {t("save")}
        </Button>
        <Button
          variant="link"
          onClick={reset}
          data-testid="OIDCAuthFlowOverrideRevert"
        >
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
