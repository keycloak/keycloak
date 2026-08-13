{
  "name": "{{name}}",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "start-keycloak": "node ./start-server.js --account-dev"
  },
  "dependencies": {
    "@keycloak/keycloak-account-ui": "{{version}}",
    "@keycloak/keycloak-ui-shared": "{{version}}",
    "@patternfly/react-core": "5.0.0",
    "i18next": "^23.10.1",
    "i18next-http-backend": "^2.5.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-i18next": "^14.1.0",
    "react-router-dom": "^6.23.1"
  },
  "devDependencies": {
    "@keycloak/keycloak-admin-client": "{{version}}",
    "@octokit/rest": "^20.1.1",
    "@types/react": "^18.2.67",
    "@types/react-dom": "^18.2.22",
    "@vitejs/plugin-react-swc": "^3.7.0",
    "gunzip-maybe": "^1.4.2",
    "tar-fs": "^3.0.6",
    "typescript": "^5.4.3",
    "vite": "^5.2.2",
    "vite-plugin-checker": "^0.6.4"
  }
}
