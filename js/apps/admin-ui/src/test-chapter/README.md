# Тестовое задание: Frontend (Keycloak Admin UI)

## Запуск локально

1.Установить зависимости
2.Поднять Keycloak-сервер

```
cd apps/keycloak-server
pnpm start --admin-dev
```

3.Запустить admin-ui 
```
cd apps/admin-ui
pnpm dev
```
Открывать через http://localhost:8080/admin/master/console/