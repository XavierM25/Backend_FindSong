# FindSong API

API de reconocimiento de música que permite identificar canciones a partir de grabaciones de audio.

## Descripción

FindSong API es un servicio backend desarrollado con Spring Boot que utiliza la API de Shazam para identificar canciones. La aplicación recibe fragmentos de audio, los procesa y devuelve información detallada sobre la canción identificada, incluyendo título, artista, álbum, y enlaces a plataformas de streaming.

## Características

- Reconocimiento de audio para identificar canciones
- Interfaz web sencilla para probar el servicio
- Autenticación mediante JWT para endpoints protegidos
- Validación de datos de entrada
- Gestión de errores centralizada
- Soporte para CORS

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.2.0
- Spring Security
- MongoDB
- JWT para autenticación
- OkHttp3 para llamadas a APIs externas
- Lombok

## Requisitos previos

- JDK 17 o superior
- Maven 3.6 o superior
- MongoDB (opcional, solo si se usa la funcionalidad de usuarios)
- Clave de API de RapidAPI para el servicio de Shazam

## Instalación

1. Clona el repositorio:
   ```bash
   git clone https://github.com/tuusuario/findsong-api.git
   cd findsong-api
   
2. Configura las propiedades de la aplicación:
   Crea o edita el archivo src/main/resources/application.properties con tu clave de API:
    ```properties
   app.rapid-api-key=tu-clave-api-aqui

3. Compila el proyecto:
   ```bash
   ./mvnw clean package
   ```

4. Ejecuta la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```
## Uso
Endpoints principales

    GET /api: Endpoint de estado para verificar que el servicio esté funcionando

    POST /api/audio-recognition/identify: Identifica una canción a partir de un archivo de audio

    POST /api/auth/login: Inicia sesión de usuario (si se implementa)

## Ejemplo de uso con cURL
   ```properties
   curl -X POST \
   http://localhost:3000/api/audio-recognition/identify \
   -H 'Content-Type: multipart/form-data' \
   -F 'audio=@ruta/a/tu/archivo/audio.mp3' \
   -F 'duration=10'
   ```

## Respuesta de ejemplo
```json
{
  "success": true,
  "song": {
    "title": "Nombre de la canción",
    "artist": "Nombre del artista",
    "album": "Nombre del álbum",
    "releaseDate": "2023",
    "genres": ["Pop"],
    "spotifyId": "identificador-spotify",
    "appleMusicId": "identificador-apple-music",
    "coverArtUrl": "https://ejemplo.com/portada.jpg",
    "previewUrl": "https://ejemplo.com/preview.mp3"
  },
  "message": "Canción identificada correctamente"
}
```

## Interfaz web
La aplicación incluye una interfaz web simple para probar el servicio. Accede a ella desde:
http://localhost:3000

## Configuración
Las principales propiedades configurables son:
```
server.port=3000
app.rapid-api-key=tu-clave-api-aqui
app.cors-origins=*
app.http.connect-timeout=30000
app.http.read-timeout=30000
app.http.write-timeout=30000
```

## Contribución
1. Haz un fork del proyecto

2. Crea una rama para tu característica:
    ```bash
    git checkout -b feature/nueva-caracteristica
    ```
   
3. Haz commit de tus cambios:
    ```bash
    git commit -am 'Añade nueva característica'
    ```
   
4. Haz push a la rama:
    ```bash
   git push origin feature/nueva-caracteristica
   ```
   
5. Crea un Pull Request

## Licencia
Este proyecto está licenciado bajo la licencia MIT.

## Contacto
Para cualquier consulta, puedes contactar al equipo de desarrollo en:
[📧 findsongsupport@example.com]