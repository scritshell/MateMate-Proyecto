# MateMate - Aplicación Android de Ajedrez

Aplicación Android nativa completa para aprender, practicar y disfrutar del ajedrez con inteligencia artificial, comunidad y análisis de partidas.

## Descripción General

**MateMate** es una aplicación Android desarrollada en **Kotlin** que ofrece:

- Motor de ajedrez con IA (Stockfish integrado)
- Comunidad y redes sociales
- Análisis de partidas
- Noticias de ajedrez en tiempo real
- Mapa de jugadores cercanos
- Soporte multimedia (audio, cámara)
- Internacionalización completa
- Sincronización en la nube con Firebase
- Interfaz responsive (teléfono, tablet, paisaje)

## Instalación Rápida

1. Descarga el APK desde [Releases](https://github.com/scritshell/MateMate-Proyecto)
2. Instálalo en tu dispositivo Android 9+

## 📦 Dependencias Principales

El proyecto utiliza las siguientes librerías (incluidas automáticamente):

| Librería | Versión | Propósito |
|----------|---------|----------|
| Jetpack Compose | Latest | UI moderna reactiva |
| Navigation Component | Latest | Navegación entre pantallas |
| Firebase Auth | Latest | Autenticación de usuarios |
| Firebase Firestore | Latest | Base de datos en la nube |
| Room | Latest | Persistencia local |
| Retrofit | Latest | Consumo de APIs REST |
| Okhttp | Latest | Cliente HTTP |
| Stockfish Engine | Latest | Motor de ajedrez IA |
| Google Maps | Latest | Mapas integrados |
| Coil | Latest | Carga de imágenes |

Ver `build.gradle.kts` para la lista completa.

## Estructura del Proyecto

```
ProyectoAjedrez/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/matmate/
│   │   │   │   ├── ui/                    # Fragmentos y Compose Screens
│   │   │   │   ├── viewmodel/             # ViewModels
│   │   │   │   ├── repository/            # Repositorios de datos
│   │   │   │   ├── model/                 # Modelos de datos
│   │   │   │   ├── util/                  # Utilidades
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                       # Recursos (layouts, strings, etc.)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                          # Pruebas unitarias
│   │   └── androidTest/                   # Pruebas instrumentadas
│   ├── build.gradle.kts                   # Configuración de Gradle
│   └── google-services.json                # Configuración de Firebase
├── gradle/
│   └── libs.versions.toml                 # Versiones de dependencias
├── build.gradle.kts                       # Configuración raíz
├── settings.gradle.kts                    # Configuración de módulos
└── README.md                              # Este archivo
```
