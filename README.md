# Balance-U App

Una aplicación móvil de bienestar físico y mental desarrollada en Kotlin para Android.

## 🎯 Descripción

Balance-U es una aplicación que promueve el bienestar físico y mental de los usuarios. Permite registrar hábitos saludables, estados de ánimo y muestra frases motivacionales. Combina seguimiento físico y emocional para fomentar constancia y equilibrio.

## 🧩 Características Técnicas

- **Lenguaje**: Kotlin
- **Arquitectura**: MVVM (Model - View - ViewModel)
- **Base de datos**: Firebase Firestore
- **Autenticación**: Firebase Authentication (correo y contraseña)
- **Librerías principales**:
  - ViewModel & LiveData
  - Firebase Authentication & Firestore
  - MPAndroidChart (para gráficas)
  - Material Components
- **SDK mínimo**: 26
- **SDK objetivo**: 34

## ⚙️ Funcionalidades

- **Pantalla de inicio**: Resumen del día con hábitos, estado de ánimo y frase motivacional
- **Gestión de hábitos**: Agregar, marcar como completado, ver progreso
- **Registro emocional**: Seleccionar estado de ánimo e ingresar una nota opcional
- **Estadísticas**: Mostrar gráficas del progreso físico y emocional
- **Autenticación**: Registro e inicio de sesión con Firebase
- **Frases motivacionales**: Cargadas desde Firebase o archivo local JSON

## 📋 Configuración

### 1. Configurar Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Agrega una aplicación Android con el package name: `com.example.balanceuapp`
3. Descarga el archivo `google-services.json`
4. Coloca el archivo en `app/google-services.json`

### 2. Habilitar servicios de Firebase

En Firebase Console, habilita:
- **Authentication**: Método Email/Password
- **Firestore Database**: Crea una base de datos en modo de prueba

### 3. Estructura de Firestore

La aplicación creará automáticamente las siguientes colecciones:
- `usuarios`: Información de los usuarios
- `habitos`: Hábitos registrados por usuario
- `estadosAnimo`: Estados de ánimo registrados
- `frasesMotivacionales`: Frases motivacionales (opcional, también se cargan desde JSON local)

### 4. Compilar y ejecutar

```bash
./gradlew build
```

O abre el proyecto en Android Studio y ejecuta la aplicación.

## 📁 Estructura del Proyecto

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/balanceuapp/
│   │   │   ├── data/
│   │   │   │   ├── model/          # Modelos de datos
│   │   │   │   └── repository/      # Repositorios para Firebase
│   │   │   ├── ui/
│   │   │   │   ├── auth/            # Activities de autenticación
│   │   │   │   ├── fragments/       # Fragments principales
│   │   │   │   └── viewmodel/       # ViewModels
│   │   │   └── BalanceUApplication.kt
│   │   ├── res/
│   │   │   ├── layout/              # Layouts XML
│   │   │   ├── menu/                 # Menús
│   │   │   ├── navigation/          # Navegación
│   │   │   └── values/               # Recursos
│   │   └── assets/
│   │       └── frases_motivacionales.json
```

## 🚀 Uso

1. **Registro/Login**: Al abrir la app, se muestra la pantalla de login. Puedes registrarte o iniciar sesión.
2. **Pantalla de Inicio**: Muestra un resumen del día con hábitos completados, estado de ánimo y una frase motivacional.
3. **Hábitos**: Agrega hábitos y márcalos como completados cuando los realices.
4. **Estado de Ánimo**: Registra cómo te sientes cada día con una nota opcional.
5. **Estadísticas**: Visualiza gráficas de tu progreso en hábitos y estados de ánimo.

## 📝 Notas

- Asegúrate de tener conexión a internet para usar Firebase
- Las frases motivacionales se cargan desde un archivo JSON local si Firebase no está disponible
- Los datos se sincronizan automáticamente con Firestore

## 🔧 Solución de Problemas

- Si la app no se conecta a Firebase, verifica que `google-services.json` esté en la ubicación correcta
- Asegúrate de que Authentication y Firestore estén habilitados en Firebase Console
- Verifica que el package name coincida con el configurado en Firebase

## 📄 Licencia

Este proyecto es un ejemplo educativo.

