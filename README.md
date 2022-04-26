# Keycloak

Keycloak es una solución de gestión de acceso e identidad de código abierto para aplicaciones y servicios modernos.

Este repositorio contiene el código fuente del servidor de Keycloak, los adaptadores de Java y el adaptador de JavaScript.

## Documentación de Apoyo

- [Documentación](https://www.keycloak.org/documentation.html)
- [Lista de correo de usuarios ](https://groups.google.com/d/forum/keycloak-user) - Lista de correo para preguntas generales y soporte sobre Keycloak

## Reportar vulnerabilidades de seguridad

Si ha encontrado una vulnerabilidad de seguridad, consulte las [instrucciones sobre cómo informarla correctamente.](https://github.com/keycloak/keycloak/security/policy)

## Reportar un problema

Si cree haber descubierto un defecto en Keycloak, abra [un problema](https://github.com/keycloak/keycloak/issues). Recuerde proporcionar un buen resumen, una descripción y los pasos para reproducir el problema.

## Primeros Pasos

Para ejecutar Keycloak, descargue la distribución desde [nuestro sitio web](https://www.keycloak.org/downloads.html). Descomprima y ejecute:

    bin/kc.[sh|bat] start-dev

Alternativamente, puede usar la imagen de Docker ejecutando:

    docker run quay.io/keycloak/keycloak start-dev

Para obtener más detalles, consulte la [documentación de Keycloak](https://www.keycloak.org/documentation.html).

## Guia de usuario

Instalación

    * Instalar JDK 11 o posterior, es necesario tenerla instalada ya que Keycloak funciona con Java.
    * Descargar  keycloak-18.0.0.zip
    * Extraer los archivos del zip keycloak-18.0.0.zip
    * Abrir el directorio de keycloak-18.0.0
    * Abrir terminal, posicionándose en la carpeta de keycloak-18.0.0
    * En Linux ejecutar: bin/kc.sh start-dev
    * En Windows ejecutar:  bin/kc.bat start-dev

Crear cuenta de administrador

Una vez terminada la instalación, puede crear su cuenta como administrador, Keycloak no tiene administrador predeterminado.
Seguir los pasos siguientes para comenzar su configuración:

    * Abra http://localhost:8080/
    * Complete el formulario con su nombre de usuario y contraseña
    * Vaya a la consola de administración de Keycloak e inicie sesión, con su usuario y contraseña.

Cree un Reino.

Un reino (realm) equivale a crear un grupo aislado de aplicaciones y usuarios. Master es el reino predeterminado y no debe usarse para sus aplicaciones.

- Posiciones en la esquina superior izquierda donde dice master, haga clic en Add realm.
- Llene el formulario con los valores: Name:myrealm.
- Haga click en crear.

Cree un Usuario

Para agregar un usuario en un reino haga lo siguiente:

- Haga click en el menú de la izquierda en Users
- Haga click en Adduser
- Llene el formulario
  Iniciar sesión en la consola de la cuenta
- Abra Keycloak Account Console
- Inicie sesión con su correo y contraseña creado previamente
  Registre una aplicación
- Haga click en clientes
- LLene el formulario con los datos de su aplicación
- Haga clic en save

## Construir desde el código fuente.

Para compilar desde el código fuente, consulte la [guía de creación y trabajo con código base](docs/building.md).

### Pruebas

Para ejecutar pruebas, consulte la [guía de pruebas](docs/tests.md).

### Escribir pruebas

Para escribir pruebas, consulte la guía de [escritura de pruebas](docs/tests-development.md).

## Contribuir

Antes de contribuir a Keycloak, lea nuestras [pautas de contribución](CONTRIBUTING.md).

## Otros Proyectos de Keycloak

- [Keycloak](https://github.com/keycloak/keycloak) - Servidor Keycloak y adaptadores Java
- [Keycloak Documentation](https://github.com/keycloak/keycloak-documentation) - Documentación para Keycloak
- [Keycloak QuickStarts](https://github.com/keycloak/keycloak-quickstarts) - Inicios rápidos para comenzar con Keycloak
- [Keycloak Node.js Connect](https://github.com/keycloak/keycloak-nodejs-connect) - Adaptador Node.js para Keycloak
- [Keycloak Node.js Admin Client](https://github.com/keycloak/keycloak-nodejs-admin-client) - Biblioteca de Node.js para Keycloak Admin REST API

## Licencia

- [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
