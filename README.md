# FindSong API

API de reconocimiento de música desarrollada con Spring Boot que permite identificar canciones mediante fragmentos de audio.

## Características

- Reconocimiento de canciones mediante la API de Shazam
- Enriquecimiento de la información con datos de Spotify
- Autenticación mediante JWT
- Caché de respuestas para mejorar el rendimiento
- Endpoints de monitoreo de salud
- Soporte para MongoDB como base de datos

## Requisitos

- Java 17 o superior
- Maven 3.8+
- MongoDB
- Cuenta en RapidAPI (Shazam)
- Cuenta en Spotify Developer

## Instalación

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/tuusuario/findsong-api.git
   cd findsong-api
   ```

2. Configurar las credenciales:

   - Copiar `src/main/resources/application.properties.example` a `src/main/resources/application.properties`
   - Actualizar las credenciales de RapidAPI y Spotify en el archivo

3. Construir el proyecto:

   ```bash
   mvn clean install
   ```

4. Ejecutar la aplicación:
   ```bash
   mvn spring-boot:run
   ```

## Uso

### Reconocimiento de canciones

```bash
curl -X POST \
  'http://localhost:3000/api/audio-recognition/identify' \
  -H 'Content-Type: multipart/form-data' \
  -F 'audio=@ruta/al/archivo/audio.mp3' \
  -F 'duration=10'
```

### Autenticación

**Registro:**

```bash
curl -X POST \
  'http://localhost:3000/api/auth/register' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "usuario",
    "email": "usuario@ejemplo.com",
    "password": "contraseña"
  }'
```

**Login:**

```bash
curl -X POST \
  'http://localhost:3000/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "usuario",
    "password": "contraseña"
  }'
```

## Endpoints Principales

- `/api/auth/register` - Registro de nuevos usuarios
- `/api/auth/login` - Autenticación de usuarios
- `/api/audio-recognition/identify` - Identificación de canciones
- `/api/health` - Estado de salud del sistema
- `/api` - Información básica del API

## Documentación Técnica

La API utiliza:

- Spring Boot 3.2 para el backend
- Spring Security + JWT para la autenticación
- MongoDB para almacenamiento
- Caffeine para caché de alta eficiencia
- OkHttp para comunicaciones HTTP
- Actuator para monitoreo

## Configuración

Ver archivo `application.properties.example` para todas las opciones disponibles.

## Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo LICENSE para más detalles.
