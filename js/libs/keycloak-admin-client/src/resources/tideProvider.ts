import type { KeycloakAdminClient } from "../client.js";
import { RequiredActionAlias } from "../defs/requiredActionProviderRepresentation.js";
import Resource from "./resource.js";

/* TIDECLOAK IMPLEMENTATION */
interface stripeCheckoutSessionResponse {
    message: string,
    activationPackage: string,
    redirectUrl: string,

}

interface License {
    licenseData: string;
    status: string;
    date: string;
}

interface licenseDetails {
    currentUserAcc: string,
    expiryDate: number,
}

interface scheduledTaskInfo {
    taskName: string,
    startDateMillis: number,
    delayMillis: number,

}

export class TideProvider extends Resource<{ realm?: string }> {
    public getRequiredActionLink = this.makeRequest<
        {
            userId: string;
            clientId?: string;
            lifespan?: number;
            redirectUri?: string;
            actions?: (RequiredActionAlias | string)[];
        },
        string
    >({
        method: "POST",
        path: "/tideAdminResources/get-required-action-link",
        payloadKey: "actions",
        queryParamKeys: ["lifespan", "redirectUri", "clientId", "userId"],
        keyTransform: {
            clientId: "client_id",
            redirectUri: "redirect_uri",
        },
    });

    public addAuthorization = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/add-authorization",
    });
    public addRejection = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/add-rejection",
    });

    public saveFirstAdminAuthorizer = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/vendorResources/first-admin-authorizer"
    });

    public getVouchers = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/new-voucher"
    });

    public rotateVrk = this.makeRequest<void, string>({
        method: "POST",
        path: "/vendorResources/rotate-vrk"
    });


    public getScheduledTasks = this.makeRequest<void, scheduledTaskInfo[]>({
        method: "GET",
        path: "/vendorResources/scheduledTasks",
    });

    public getLicenseHistory = this.makeRequest<void, License[]>({
        method: "GET",
        path: "/vendorResources/licenseHistory",
    });


    public triggerScheduledTask = this.makeRequest<{ taskName: string }, Response>({
        method: "POST",
        path: "/vendorResources/scheduledTasks/{taskName}/trigger",
        urlParamKeys: ["taskName"],
        catchNotFound: true,
    });


    public scheduleGenVRKTask = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/scheduledTasks/genVRK/schedule",
    });


    public uploadImage = this.makeRequest<FormData, Record<string, string>>({
        method: "POST",
        path: "/tide-idp-admin-resources/images/upload",
    });

    public getImageName = this.makeRequest<{ type: string }, string | null>({
        method: "GET",
        path: "/tide-idp-admin-resources/images/{type}/name",
        urlParamKeys: ["type"],
        catchNotFound: true,
    });

    public deleteImage = this.makeRequest<{ type: string }, Response>({
        method: "DELETE",
        path: "/tide-idp-admin-resources/images/{type}/delete",
        urlParamKeys: ["type"],
        catchNotFound: true,
    });

    public generateInitialKey = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/generate-initial-key",
    });

    public signIdpSettings = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/sign-idp-settings",
    });

    public generateInitialVrk = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/generate-initial-vrk",
    });

    public confirmInitialVrk = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/confirm-initial-vrk",
    });

    public clearTempVrk = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/clear-temp-vrk",
    });


    public generateVendorId = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/generate-vendor-id",
    });

    public signMessage = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/sign-message",
    });

    public authorizeStripeRequest = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/authorize-stripe-request",
    });

    public createStripeCheckoutSession = this.makeRequest<FormData, stripeCheckoutSessionResponse>({
        method: "POST",
        path: "/vendorResources/createStripeCheckoutSession",
    });

    public isPendingLicenseActive = this.makeRequest<void, boolean>({
        method: "GET",
        path: "/vendorResources/isPendingLicenseActive",
    });

    public getLicenseDetails = this.makeRequest<void, licenseDetails>({
        method: "GET",
        path: "/vendorResources/getLicenseDetails",
    });

    public getSubscriptionStatus = this.makeRequest<void, Response>({
        method: "GET",
        path: "/vendorResources/getSubscriptionStatus",
    });

    public createCustomerPortalSession = this.makeRequest<FormData, stripeCheckoutSessionResponse>({
        method: "POST",
        path: "/vendorResources/createCustomerPortalSession",
    });

    public updateSubscription = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/updateSubscription",
    });

    public cancelSubscription = this.makeRequest<void, Response>({
        method: "GET",
        path: "/vendorResources/cancelSubscription",
    });

    public getInstallationProviders = this.makeRequest<
        { clientId: string; providerId: string },
        string
    >({
        method: "GET",
        path: "/vendorResources/get-installations-provider",
        queryParamKeys: ["clientId", "providerId"],
    });


    public getTideJwk = this.makeRequest<void, Response>({
        method: "GET",
        path: "/vendorResources/get-tide-jwk",
    });

    public toggleIGA = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/tide-admin/toggle-iga",
    });

    public triggerLicenseRenewedEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerLicenseRenewedEvent/{error}"
    })
    public triggerVendorKeyCreationEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerVendorKeyCreationEvent/{error}"
    })
    public triggerAuthorizerUpdateEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerAuthorizerUpdateEvent/{error}"
    })
    public triggerAuthorizeEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerAuthorizeEvent/{error}"
    })
    constructor(client: KeycloakAdminClient) {
        super(client, {
            path: "/admin/realms/{realm}",
            getUrlParams: () => ({
                realm: client.realmName,
            }),
            getBaseUrl: () => client.baseUrl,
        });
    }
}