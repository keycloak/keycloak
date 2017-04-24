/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
declare module KeycloakModule {

    export interface Promise {
        success(callback: Function): Promise;
        error(callback: Function): Promise;
    }

    export type ResponseModes = "query" | "fragment";
    export type Flows = "standard" | "implicit" | "hybrid";
    
    export interface InitOptions {
        checkLoginIframe?: boolean;
        checkLoginIframeInterval?: number;
        onLoad?: string;
        adapter?: string;
        responseMode?: ResponseModes;
        flow?: Flows;
        token?: string;
        refreshToken?: string;
        idToken?: string;
        timeSkew?: number;
    }

    export interface LoginOptions {
        redirectUri?: string;
        prompt?: string;
        maxAge?: number;
        loginHint?: string;
        action?: string;
        locale?: string;
    }

    export interface RedirectUriOptions {
        redirectUri?: string;
    }

    export interface KeycloakClient {
        init(options?: InitOptions): Promise;
        login(options?: LoginOptions): Promise;
        createLoginUrl(options?: LoginOptions): string;
        logout(options?: RedirectUriOptions): Promise;
        createLogoutUrl(options?: RedirectUriOptions): string;
        register(options?: LoginOptions): Promise;
        createRegisterUrl(options?: RedirectUriOptions): string;
        accountManagement(): Promise;
        createAccountUrl(options?: RedirectUriOptions): string;
        hasRealmRole(role: string): boolean;
        hasResourceRole(role: string, resource?: string): boolean;
        loadUserProfile(): Promise;
        isTokenExpired(minValidity: number): boolean;
        updateToken(minValidity: number): Promise;
        clearToken(): any;

        realm: string;
        clientId: string;
        authServerUrl: string;

        token: string;
        tokenParsed: any;
        refreshToken: string;
        refreshTokenParsed: any;
        idToken: string;
        idTokenParsed: any;
        realmAccess: any;
        resourceAccess: any;
        authenticated: boolean;
        subject: string;
        timeSkew: number;
        responseMode: ResponseModes;
        flow: Flows;
        responseType: string;

        onReady: Function;
        onAuthSuccess: Function;
        onAuthError: Function;
        onAuthRefreshSuccess: Function;
        onAuthRefreshError: Function;
        onAuthLogout: Function;
        onTokenExpired: Function;
    }
}

declare var Keycloak: {
    new(config?: any): KeycloakModule.KeycloakClient;
};