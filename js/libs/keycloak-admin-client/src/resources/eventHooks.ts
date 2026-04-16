import type { KeycloakAdminClient } from "../client.js";
import type EventHookLogRepresentation from "../defs/eventHookLogRepresentation.js";
import type EventHookMessageRepresentation from "../defs/eventHookMessageRepresentation.js";
import type EventHookProviderRepresentation from "../defs/eventHookProviderRepresentation.js";
import type EventHookTargetRepresentation from "../defs/eventHookTargetRepresentation.js";
import type EventHookTestResultRepresentation from "../defs/eventHookTestResultRepresentation.js";
import Resource from "./resource.js";

interface EventHookTargetQuery {
    realm?: string;
}

interface EventHookTargetByIdQuery extends EventHookTargetQuery {
    targetId: string;
}

interface EventHookMessageQuery extends EventHookTargetQuery {
    status?: string;
    targetId?: string;
    first?: number;
    max?: number;
}

interface EventHookMessageByIdQuery extends EventHookTargetQuery {
    messageId: string;
}

interface EventHookLogQuery extends EventHookTargetQuery {
    targetId?: string;
    targetType?: string;
    executionId?: string;
    search?: string;
    messageId?: string;
    first?: number;
    max?: number;
}

interface EventHookMessageLogQuery extends EventHookMessageByIdQuery {
    first?: number;
    max?: number;
}

export class EventHooks extends Resource<{ realm?: string }> {
    constructor(client: KeycloakAdminClient) {
        super(client, {
            path: "/admin/realms/{realm}/event-hooks",
            getUrlParams: () => ({
                realm: client.realmName,
            }),
            getBaseUrl: () => client.baseUrl,
        });
    }

    public findTargets = this.makeRequest<
        EventHookTargetQuery,
        EventHookTargetRepresentation[]
    >({
        method: "GET",
        path: "/targets",
    });

    public findTarget = this.makeRequest<
        EventHookTargetByIdQuery,
        EventHookTargetRepresentation | undefined
    >({
        method: "GET",
        path: "/targets/{targetId}",
        urlParamKeys: ["targetId"],
        catchNotFound: true,
    });

    public createTarget = this.makeRequest<
        EventHookTargetRepresentation,
        EventHookTargetRepresentation
    >({
        method: "POST",
        path: "/targets",
    });

    public updateTarget = this.makeUpdateRequest<
        EventHookTargetByIdQuery,
        EventHookTargetRepresentation,
        EventHookTargetRepresentation
    >({
        method: "PUT",
        path: "/targets/{targetId}",
        urlParamKeys: ["targetId"],
    });

    public delTarget = this.makeRequest<EventHookTargetByIdQuery, void>({
        method: "DELETE",
        path: "/targets/{targetId}",
        urlParamKeys: ["targetId"],
    });

    public testTarget = this.makeRequest<
        EventHookTargetRepresentation,
        EventHookTestResultRepresentation
    >({
        method: "POST",
        path: "/targets/test",
    });

    public findProviders = this.makeRequest<
        EventHookTargetQuery,
        EventHookProviderRepresentation[]
    >({
        method: "GET",
        path: "/providers",
    });

    public findMessages = this.makeRequest<
        EventHookMessageQuery,
        EventHookMessageRepresentation[]
    >({
        method: "GET",
        path: "/messages",
    });

    public findMessage = this.makeRequest<
        EventHookMessageByIdQuery,
        EventHookMessageRepresentation | undefined
    >({
        method: "GET",
        path: "/messages/{messageId}",
        urlParamKeys: ["messageId"],
        catchNotFound: true,
    });

    public retryMessage = this.makeRequest<
        EventHookMessageByIdQuery,
        EventHookMessageRepresentation
    >({
        method: "POST",
        path: "/messages/{messageId}/retry",
        urlParamKeys: ["messageId"],
    });

    public findMessageLogs = this.makeRequest<
        EventHookMessageLogQuery,
        EventHookLogRepresentation[]
    >({
        method: "GET",
        path: "/messages/{messageId}/logs",
        urlParamKeys: ["messageId"],
    });

    public findLogs = this.makeRequest<
        EventHookLogQuery,
        EventHookLogRepresentation[]
    >({
        method: "GET",
        path: "/logs",
    });
}