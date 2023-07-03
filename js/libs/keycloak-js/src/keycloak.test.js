const Keycloak = require('./keycloak.js');

const mockServer = {
    url: 'http://localhost:8080/auth',
    realm: 'test',
    clientId: 'test-client'
};

const mockInitOptions = {
    onLoad: 'login-required',
    checkLoginIframe: false
};

const kc = new Keycloak(mockServer);

kc.init(mockInitOptions).then(() => {
    expect(kc.authenticated).toBe(true);

    kc.init(mockInitOptions).catch((error) => {
        expect(error).toBeDefined();
        expect(error.message).toBe('Keycloak has already been initialized.');
    });
});