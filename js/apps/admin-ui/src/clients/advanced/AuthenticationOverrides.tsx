import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { SelectControl, useFetch } from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button } from "@patternfly/react-core";
import { sortBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";

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

  const { t } = useTranslation();
  const [flows, setFlows] = useState<AuthenticationFlowRepresentation[]>([]);

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) => {
      let filteredFlows = [
        ...flows.filter((flow) => flow.providerId !== "client-flow"),
      ];
      filteredFlows = sortBy(filteredFlows, [(f) => f.alias]);
      setFlows(filteredFlows);
    },
    [],
  );

  return (
    <FormAccess
      role="manage-clients"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      <SelectControl
        name="authenticationFlowBindingOverrides.browser"
        label={t("browserFlow")}
        labelIcon={t("browserFlowHelp")}
        controller={{
          defaultValue: "",
        }}
        options={[
          { key: "", value: t("choose") },
          ...flows.map(({ id, alias }) => ({ key: id!, value: alias! })),
        ]}
      />
      {protocol === "openid-connect" && (
        <SelectControl
          name="authenticationFlowBindingOverrides.direct_grant"
          label={t("directGrant")}
          labelIcon={t("directGrantHelp")}
          controller={{
            defaultValue: "",
          }}
          options={[
            { key: "", value: t("choose") },
            ...flows.map(({ id, alias }) => ({ key: id!, value: alias! })),
          ]}
        />
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
