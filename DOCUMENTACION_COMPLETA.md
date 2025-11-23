# 📚 Documentación Completa - Balance-U App

## 1. DESCRIPCIÓN GENERAL DEL SISTEMA

### 1.1 ¿Qué es Balance-U?

**Balance-U** es una aplicación móvil Android desarrollada en Kotlin que promueve el bienestar físico y mental de los usuarios. La aplicación permite:

- **Registrar y gestionar hábitos saludables**: Crear, editar, eliminar y marcar hábitos como completados
- **Registrar estados de ánimo**: Seleccionar cómo se siente el usuario cada día con notas opcionales
- **Visualizar resumen diario**: Ver hábitos completados, estado de ánimo del día y frases motivacionales
- **Autenticación de usuarios**: Sistema de registro e inicio de sesión seguro

### 1.2 Arquitectura del Sistema

La aplicación sigue la **arquitectura MVVM (Model-View-ViewModel)**:

```
┌─────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                  │
│  Activities & Fragments (UI)                            │
│  - AuthActivity, MainActivity                           │
│  - InicioFragment, HabitosFragment, etc.               │
└──────────────────┬──────────────────────────────────────┘
                   │ Observa LiveData
                   ▼
┌─────────────────────────────────────────────────────────┐
│                    CAPA DE LÓGICA                      │
│  ViewModels (Lógica de negocio)                        │
│  - AuthViewModel, InicioViewModel, HabitoViewModel    │
│  - EstadoAnimoViewModel                                │
└──────────────────┬──────────────────────────────────────┘
                   │ Usa Repositories
                   ▼
┌─────────────────────────────────────────────────────────┐
│                    CAPA DE DATOS                        │
│  Repositories (Acceso a datos)                        │
│  - AuthRepository, HabitoRepository                    │
│  - EstadoAnimoRepository, FraseRepository              │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA                 │
│  Firebase (Backend)                                    │
│  - Firebase Authentication (usuarios)                    │
│  - Firestore (base de datos)                          │
└─────────────────────────────────────────────────────────┘
```

### 1.3 Tecnologías Utilizadas

- **Lenguaje**: Kotlin
- **Arquitectura**: MVVM
- **Base de datos**: Firebase Firestore
- **Autenticación**: Firebase Authentication
- **UI**: Material Design Components
- **Navegación**: Navigation Component
- **Binding**: View Binding
- **Gráficas**: MPAndroidChart
- **JSON**: Gson

---

## 2. FLUJO PRINCIPAL DE LA APLICACIÓN

### 2.1 Flujo de Inicio de la Aplicación

```
1. Usuario abre la app
   ↓
2. Android lanza AuthActivity (actividad principal en manifest)
   ↓
3. AuthActivity muestra ViewPager2 con dos tabs:
   - Tab 1: LoginFragment (Iniciar sesión)
   - Tab 2: RegisterFragment (Registrarse)
   ↓
4. Usuario puede:
   a) Iniciar sesión → AuthViewModel.iniciarSesion()
   b) Registrarse → AuthViewModel.registrarUsuario()
   ↓
5. Si la autenticación es exitosa:
   - AuthActivity navega a MainActivity
   - Se limpia el stack de actividades
   ↓
6. MainActivity verifica sesión:
   - Si hay sesión → Continúa
   - Si no hay sesión → Redirige a AuthActivity
   ↓
7. MainActivity muestra InicioFragment (por defecto)
   - Carga resumen del día
   - Observa hábitos en tiempo real
   - Muestra frase motivacional
```

### 2.2 Flujo de Registro de Estado de Ánimo

```
Usuario en InicioFragment
   ↓
1. Usuario selecciona estado de ánimo (5 opciones)
   ↓
2. Usuario escribe nota opcional
   ↓
3. Usuario presiona "Guardar"
   ↓
4. InicioFragment valida:
   - ¿Hay estado seleccionado? → Si no, muestra error
   ↓
5. InicioFragment crea objeto EstadoAnimo
   ↓
6. InicioFragment llama: estadoAnimoViewModel.registrarEstadoAnimo()
   ↓
7. EstadoAnimoViewModel (coroutine) → EstadoAnimoRepository
   ↓
8. EstadoAnimoRepository guarda en Firestore:
   collection("estadosAnimo").add(estadoAnimo)
   ↓
9. Resultado → LiveData en ViewModel
   ↓
10. InicioFragment observa → Actualiza UI:
    - Muestra mensaje de éxito
    - Limpia formulario
    - Recarga resumen del día
```

### 2.3 Flujo de Gestión de Hábitos

```
Usuario en HabitosFragment
   ↓
1. Fragment inicia observación en tiempo real:
   habitoViewModel.startObservandoHabitos(userId)
   ↓
2. HabitoRepository configura listener de Firestore
   ↓
3. Cualquier cambio en Firestore → Callback automático
   ↓
4. Repository actualiza lista → ViewModel.postValue()
   ↓
5. LiveData notifica → HabitosFragment observa
   ↓
6. RecyclerView se actualiza automáticamente
   ↓
7. Usuario puede:
   a) Agregar hábito → Dialog → HabitoViewModel.agregarHabito()
   b) Editar hábito → Dialog → HabitoViewModel.actualizarHabito()
   c) Marcar completado → Switch → HabitoViewModel.marcarCompletado()
   d) Eliminar hábito → Dialog → HabitoViewModel.eliminarHabito()
   ↓
8. Cada operación actualiza Firestore
   ↓
9. Listener detecta cambio → UI se actualiza automáticamente
```

### 2.4 Flujo de Observación en Tiempo Real

```
InicioFragment.onViewCreated()
   ↓
inicioViewModel.startObservandoHabitosDelDia(userId)
   ↓
HabitoRepository.observarHabitosDelDia()
   ↓
Firestore Query con listener:
   - whereEqualTo("usuarioId", userId)
   - whereGreaterThanOrEqualTo("fechaCreacion", inicioDia)
   - whereLessThanOrEqualTo("fechaCreacion", finDia)
   ↓
Firestore Listener activo
   ↓
[Cuando hay cambio en Firestore]
   ↓
Firestore ejecuta callback automáticamente
   ↓
Repository procesa documentos → Lista de Habitos
   ↓
Repository llama onUpdate(lista)
   ↓
ViewModel._habitosDelDia.postValue(lista)
   ↓
LiveData notifica a observadores
   ↓
InicioFragment actualiza contador: "X/Y hábitos completados"
```

---

## 3. DOCUMENTACIÓN POR ARCHIVO

### 3.1 Modelos de Datos

#### `Usuario.kt`
**Ubicación**: `data/model/Usuario.kt`

**¿Qué hace?**
Representa un usuario del sistema con sus datos básicos.

**Estructura**:
- `id`: Identificador único (generado por Firebase)
- `email`: Correo electrónico
- `nombre`: Nombre completo
- `fechaRegistro`: Timestamp de registro

**Métodos clave**:
- `toMap()`: Convierte a Map para guardar en Firestore
- `fromMap()`: Crea Usuario desde Map de Firestore

**Interacciones**:
- Usado por `AuthRepository` para guardar datos de usuario (actualmente comentado)
- Potencial uso futuro para perfil de usuario

---

#### `Habito.kt`
**Ubicación**: `data/model/Habito.kt`

**¿Qué hace?**
Representa un hábito que el usuario quiere desarrollar o mantener.

**Estructura**:
- `id`: Identificador único
- `usuarioId`: ID del usuario propietario
- `nombre`: Nombre del hábito
- `descripcion`: Descripción opcional
- `fechaCreacion`: Cuándo se creó
- `completado`: Si está completado hoy
- `fechaCompletado`: Cuándo se completó (null si no está completado)

**Métodos clave**:
- `toMap()`: Serialización para Firestore
- `fromMap()`: Deserialización desde Firestore

**Interacciones**:
- Usado por `HabitoRepository` para todas las operaciones CRUD
- Usado por `HabitoViewModel` para exponer datos a la UI
- Usado por `InicioViewModel` para mostrar resumen del día

---

#### `EstadoAnimo.kt`
**Ubicación**: `data/model/EstadoAnimo.kt`

**¿Qué hace?**
Representa un registro de cómo se siente el usuario en un momento específico.

**Estructura**:
- `id`: Identificador único
- `usuarioId`: ID del usuario
- `tipo`: Enum `TipoEstadoAnimo` (ALEGRE, FELIZ, NEUTRAL, TRISTE, TERRIBLE)
- `nota`: Nota opcional del usuario
- `fecha`: Timestamp del registro

**Métodos clave**:
- `toMap()`: Serialización (convierte enum a String)
- `fromMap()`: Deserialización (convierte String a enum con manejo de errores)

**Interacciones**:
- Usado por `EstadoAnimoRepository` para CRUD
- Usado por `EstadoAnimoViewModel` y `InicioViewModel`
- Mostrado en `InicioFragment` con emojis y colores

---

#### `FraseMotivacional.kt`
**Ubicación**: `data/model/FraseMotivacional.kt`

**¿Qué hace?**
Representa una frase motivacional para mostrar al usuario.

**Estructura**:
- `id`: Identificador único
- `frase`: Texto de la frase
- `autor`: Autor de la frase

**Interacciones**:
- Usado por `FraseRepository` para cargar desde Firebase o JSON local
- Mostrado en `InicioFragment` como recomendación diaria

---

### 3.2 Repositories (Capa de Datos)

#### `AuthRepository.kt`
**Ubicación**: `data/repository/AuthRepository.kt`

**¿Qué hace?**
Maneja todas las operaciones de autenticación con Firebase.

**Métodos principales**:

1. **`registrarUsuario(email, password, nombre)`**
   - Crea usuario en Firebase Authentication
   - Retorna Result con userId
   - Nota: La creación del documento en Firestore está comentada

2. **`iniciarSesion(email, password)`**
   - Autentica usuario existente
   - Retorna Result con userId

3. **`cerrarSesion()`**
   - Cierra sesión en Firebase Auth

4. **`obtenerUsuarioActual()`**
   - Retorna userId del usuario autenticado o null

5. **`obtenerUsuario(userId)`**
   - Obtiene datos completos del usuario desde Firestore

**Interacciones**:
- Usado por `AuthViewModel` exclusivamente
- Se comunica con Firebase Auth y Firestore

**Detalles técnicos**:
- Usa coroutines con `await()` para operaciones asíncronas
- Manejo de errores con `Result<T>`
- Lazy initialization de Firebase instances

---

#### `HabitoRepository.kt`
**Ubicación**: `data/repository/HabitoRepository.kt`

**¿Qué hace?**
Gestiona todas las operaciones CRUD de hábitos en Firestore.

**Métodos principales**:

1. **`agregarHabito(habito)`**
   - Crea documento en collection "habitos"
   - Genera ID automáticamente
   - Retorna Result con ID

2. **`obtenerHabitos(usuarioId)`**
   - Obtiene todos los hábitos del usuario
   - Ordenados por fechaCreacion descendente

3. **`actualizarHabito(habito)`**
   - Actualiza documento existente

4. **`marcarCompletado(habitoId, completado)`**
   - Actualiza solo campos `completado` y `fechaCompletado`
   - Optimizado para actualizaciones rápidas

5. **`eliminarHabito(habitoId)`**
   - Elimina documento de Firestore

6. **`obtenerHabitosDelDia(usuarioId, fechaInicio, fechaFin)`**
   - Obtiene hábitos creados en un rango de fechas
   - Usado para resumen diario

7. **`observarHabitos(usuarioId, onUpdate, onError)`**
   - Configura listener en tiempo real
   - Retorna `ListenerRegistration` para poder detenerlo
   - Se ejecuta automáticamente cuando hay cambios

8. **`observarHabitosDelDia(...)`**
   - Similar a `observarHabitos` pero con filtro de fechas

**Interacciones**:
- Usado por `HabitoViewModel` e `InicioViewModel`
- Los listeners se usan para actualizaciones en tiempo real

**Detalles técnicos**:
- Usa queries de Firestore con `whereEqualTo`, `whereGreaterThanOrEqualTo`, etc.
- Los listeners se deben detener manualmente para evitar memory leaks
- Usa `DiffUtil` implícitamente a través de LiveData

---

#### `EstadoAnimoRepository.kt`
**Ubicación**: `data/repository/EstadoAnimoRepository.kt`

**¿Qué hace?**
Gestiona operaciones CRUD de estados de ánimo.

**Métodos principales**:

1. **`agregarEstadoAnimo(estadoAnimo)`**
   - Crea nuevo registro en collection "estadosAnimo"

2. **`obtenerEstadosAnimo(usuarioId)`**
   - Obtiene todos los estados de ánimo del usuario
   - Ordenados por fecha descendente

3. **`obtenerEstadoAnimoDelDia(usuarioId, fechaInicio, fechaFin)`**
   - Obtiene el estado de ánimo registrado para un día específico
   - Usa `limit(1)` para obtener solo el más reciente
   - Retorna `EstadoAnimo?` (puede ser null si no hay registro)

4. **`obtenerEstadosAnimoPorRango(usuarioId, fechaInicio, fechaFin)`**
   - Obtiene estados de ánimo en un rango de fechas
   - Útil para gráficas y estadísticas

5. **`observarEstadosAnimoPorRango(...)`**
   - Listener en tiempo real para un rango de fechas

**Interacciones**:
- Usado por `EstadoAnimoViewModel` e `InicioViewModel`

---

#### `FraseRepository.kt`
**Ubicación**: `data/repository/FraseRepository.kt`

**¿Qué hace?**
Gestiona frases motivacionales con fallback a JSON local.

**Estrategia de carga**:
1. Intenta cargar desde Firebase Firestore
2. Si falla o no hay datos, carga desde `assets/frases_motivacionales.json`
3. Selecciona una frase aleatoria

**Métodos principales**:

1. **`obtenerFraseAleatoria()`**
   - Intenta Firebase primero
   - Si falla, llama a `cargarDesdeJSON()`
   - Retorna frase aleatoria

2. **`cargarDesdeJSON()`** (privado)
   - Lee archivo JSON desde assets
   - Usa Gson para deserializar
   - Retorna frase aleatoria

**Interacciones**:
- Usado por `InicioViewModel` para cargar frase del día
- Requiere `Context` para acceder a assets

---

### 3.3 ViewModels (Capa de Lógica)

#### `AuthViewModel.kt`
**Ubicación**: `ui/viewmodel/AuthViewModel.kt`

**¿Qué hace?**
Gestiona la lógica de autenticación y expone estado a la UI.

**LiveData expuestos**:
- `registroResult: LiveData<Result<String>>` - Resultado del registro
- `loginResult: LiveData<Result<String>>` - Resultado del login
- `usuarioActual: LiveData<String?>` - ID del usuario actual

**Métodos principales**:

1. **`registrarUsuario(email, password, nombre)`**
   - Llama a `AuthRepository.registrarUsuario()`
   - Actualiza `_registroResult` y `_usuarioActual`

2. **`iniciarSesion(email, password)`**
   - Llama a `AuthRepository.iniciarSesion()`
   - Actualiza `_loginResult` y `_usuarioActual`

3. **`cerrarSesion()`**
   - Llama a `AuthRepository.cerrarSesion()`
   - Limpia `_usuarioActual`

4. **`verificarSesion(): Boolean`**
   - Verifica si hay usuario autenticado
   - Usado por `MainActivity` al iniciar

**Interacciones**:
- Observado por `AuthActivity` (LoginFragment, RegisterFragment)
- Usado por `MainActivity` para verificar sesión
- Usado por otros fragments para obtener userId

---

#### `InicioViewModel.kt`
**Ubicación**: `ui/viewmodel/InicioViewModel.kt`

**¿Qué hace?**
Gestiona la lógica de la pantalla de inicio (resumen del día).

**LiveData expuestos**:
- `habitosDelDia: LiveData<List<Habito>>` - Hábitos del día actual
- `estadoAnimoDelDia: LiveData<EstadoAnimo?>` - Estado de ánimo del día
- `fraseMotivacional: LiveData<FraseMotivacional?>` - Frase del día
- `error: LiveData<String?>` - Errores

**Métodos principales**:

1. **`cargarResumenDelDia(usuarioId)`**
   - Carga estado de ánimo del día
   - Carga frase motivacional aleatoria
   - Ejecuta en paralelo (ambas operaciones independientes)

2. **`startObservandoHabitosDelDia(usuarioId)`**
   - Inicia listener en tiempo real de hábitos del día
   - Calcula límites del día (inicio y fin)
   - Se debe llamar a `stopObservandoHabitosDelDia()` antes

3. **`stopObservandoHabitosDelDia()`**
   - Detiene listener para evitar memory leaks
   - Se llama automáticamente en `onCleared()`

4. **`obtenerHabitossCompletados(): Int`**
   - Cuenta hábitos completados de la lista actual

5. **`obtenerTotalHabitoss(): Int`**
   - Retorna total de hábitos del día

**Interacciones**:
- Usado por `InicioFragment`
- Usa `HabitoRepository`, `EstadoAnimoRepository`, `FraseRepository`

**Detalles técnicos**:
- Calcula límites del día usando módulo: `fecha - (fecha % MILISEGUNDOS_EN_UN_DIA)`
- El listener se detiene automáticamente cuando el ViewModel se destruye

---

#### `HabitoViewModel.kt`
**Ubicación**: `ui/viewmodel/HabitoViewModel.kt`

**¿Qué hace?**
Gestiona la lógica de CRUD de hábitos.

**LiveData expuestos**:
- `habitos: LiveData<List<Habito>>` - Lista completa de hábitos
- `error: LiveData<String?>` - Errores
- `operacionExitosa: LiveData<Boolean>` - Si la última operación fue exitosa

**Métodos principales**:

1. **`agregarHabito(habito)`**
   - Llama a `HabitoRepository.agregarHabito()`
   - Actualiza `_operacionExitosa`

2. **`actualizarHabito(habito)`**
   - Actualiza hábito existente

3. **`marcarCompletado(habitoId, completado, usuarioId)`**
   - Marca hábito como completado o no
   - El parámetro `usuarioId` no se usa (mantenido por compatibilidad)

4. **`eliminarHabito(habitoId, usuarioId)`**
   - Elimina hábito
   - Similar al anterior, `usuarioId` no se usa

5. **`startObservandoHabitos(usuarioId)`**
   - Inicia listener en tiempo real de TODOS los hábitos
   - Se actualiza automáticamente cuando hay cambios

6. **`stopObservandoHabitos()`**
   - Detiene listener

**Interacciones**:
- Usado por `HabitosFragment`
- El listener mantiene la lista actualizada automáticamente

---

#### `EstadoAnimoViewModel.kt`
**Ubicación**: `ui/viewmodel/EstadoAnimoViewModel.kt`

**¿Qué hace?**
Gestiona la lógica de estados de ánimo.

**LiveData expuestos**:
- `estadoAnimoActual: LiveData<EstadoAnimo?>` - Estado actual del día
- `estadosAnimo: LiveData<List<EstadoAnimo>>` - Lista de estados
- `error: LiveData<String?>` - Errores
- `operacionExitosa: LiveData<Boolean?>` - null = sin operación, true/false = resultado

**Métodos principales**:

1. **`registrarEstadoAnimo(estadoAnimo)`**
   - Guarda nuevo estado de ánimo
   - Después de guardar, recarga el estado del día automáticamente
   - Actualiza `_operacionExitosa`

2. **`limpiarOperacionExitosa()`**
   - Establece `_operacionExitosa` en null
   - Usado después de procesar el resultado

3. **`obtenerEstadoAnimoDelDia(usuarioId, fecha)`**
   - Calcula límites del día y obtiene estado
   - Usa la misma lógica de cálculo que `InicioViewModel`

**Interacciones**:
- Usado por `InicioFragment` para registrar estados de ánimo

---

### 3.4 Activities

#### `AuthActivity.kt`
**Ubicación**: `ui/auth/AuthActivity.kt`

**¿Qué hace?**
Activity principal de autenticación. Muestra login y registro en tabs.

**Estructura**:
- Usa `ViewPager2` con `TabLayout` para alternar entre login y registro
- `AuthPagerAdapter` maneja los dos fragments

**Flujo**:

1. **onCreate()**
   - Infla layout
   - Configura ViewPager con tabs
   - Configura observadores de ViewModel

2. **setupViewPager()**
   - Crea adapter con `LoginFragment` y `RegisterFragment`
   - Conecta `TabLayout` con `ViewPager2`

3. **setupObservers()**
   - Observa `loginResult` y `registroResult`
   - Si éxito → navega a `MainActivity`
   - Si error → muestra mensaje

4. **showProgress()**
   - Muestra progress bar
   - Llamado por fragments cuando inician operación

5. **navigateToMain()**
   - Crea Intent a `MainActivity`
   - Limpia stack de actividades
   - Finaliza `AuthActivity`

**Interacciones**:
- Contiene `LoginFragment` y `RegisterFragment`
- Usa `AuthViewModel` compartido (activityViewModels)
- Navega a `MainActivity` en éxito

---

#### `MainActivity.kt`
**Ubicación**: `MainActivity.kt` (raíz)

**¿Qué hace?**
Activity principal de la aplicación. Gestiona navegación y verifica autenticación.

**Flujo**:

1. **onCreate()**
   - Infla layout
   - Obtiene `AuthViewModel`
   - Verifica autenticación
   - Si no autenticado → redirige a `AuthActivity`
   - Si autenticado → configura navegación

2. **verificarAutenticacion()**
   - Llama a `authViewModel.verificarSesion()`
   - Si false → redirige a login
   - Maneja errores

3. **setupNavigation()**
   - Obtiene `NavHostFragment`
   - Conecta `BottomNavigationView` con `NavController`
   - Permite navegar entre fragments

4. **cerrarSesion()** (desde menú)
   - Cierra sesión en ViewModel
   - Redirige a login

**Interacciones**:
- Contiene `NavHostFragment` que muestra fragments
- Usa `AuthViewModel` para verificar sesión
- Gestiona navegación con Navigation Component

---

### 3.5 Fragments

#### `LoginFragment.kt`
**Ubicación**: `ui/auth/LoginFragment.kt`

**¿Qué hace?**
Fragment para iniciar sesión.

**Funcionalidad**:
- Campos: email y password
- Botón "Iniciar sesión"
- Validación de campos vacíos
- Llama a `authViewModel.iniciarSesion()`
- Muestra progress bar en `AuthActivity`

**Interacciones**:
- Usa `AuthViewModel` compartido (activityViewModels)
- Comunica con `AuthActivity` para mostrar progress

---

#### `RegisterFragment.kt`
**Ubicación**: `ui/auth/RegisterFragment.kt`

**¿Qué hace?**
Fragment para registrar nuevos usuarios.

**Funcionalidad**:
- Campos: nombre, email, password
- Validación:
  - Campos no vacíos
  - Password mínimo 6 caracteres
- Llama a `authViewModel.registrarUsuario()`

**Interacciones**:
- Similar a `LoginFragment`

---

#### `InicioFragment.kt`
**Ubicación**: `ui/fragments/InicioFragment.kt`

**¿Qué hace?**
Fragment principal que muestra resumen del día.

**Componentes UI**:

1. **Saludo personalizado**
   - Según hora del día (buenos días/tardes/noches)
   - Calculado con `Calendar.HOUR_OF_DAY`

2. **Selector de estado de ánimo**
   - 5 botones con emojis
   - Mapa `moodViews` relaciona tipo con View
   - `updateMoodSelection()` actualiza colores

3. **Campo de nota**
   - Texto opcional para el estado de ánimo

4. **Resumen de hábitos**
   - Muestra "X/Y hábitos completados"
   - Se actualiza en tiempo real

5. **Frase motivacional**
   - Cargada aleatoriamente cada día

**Flujo de inicialización**:

```
onViewCreated()
   ↓
inicializarViewModels()
   ↓
configurarUI()
   ├─ setupMoodSelector()
   ├─ setupListeners()
   └─ setupObservers()
   ↓
cargarDatosDelUsuario()
   ├─ setupSaludoPersonalizado()
   ├─ inicioViewModel.startObservandoHabitosDelDia()
   └─ inicioViewModel.cargarResumenDelDia()
```

**Observadores**:

1. **habitosDelDia**
   - Actualiza contador de hábitos completados
   - Se actualiza automáticamente por listener

2. **estadoAnimoDelDia**
   - Si hay registro → muestra estado y nota
   - Si no hay → muestra "Sin registrar"
   - Pre-selecciona el estado si existe

3. **fraseMotivacional**
   - Muestra frase o mensaje por defecto

4. **operacionExitosa** (de EstadoAnimoViewModel)
   - Si true → muestra mensaje, limpia formulario, recarga resumen

**Interacciones**:
- Usa `InicioViewModel`, `EstadoAnimoViewModel`, `AuthViewModel`
- Observa múltiples LiveData
- Inicia y detiene listeners en tiempo real

**Limpieza**:
- `onDestroyView()` → detiene observación de hábitos
- Libera binding

---

#### `HabitosFragment.kt`
**Ubicación**: `ui/fragments/HabitosFragment.kt`

**¿Qué hace?**
Fragment para gestionar lista completa de hábitos.

**Componentes**:

1. **RecyclerView**
   - Muestra lista de hábitos
   - Usa `HabitosAdapter` con `DiffUtil`
   - `LinearLayoutManager` vertical

2. **FAB (Floating Action Button)**
   - Abre diálogo para agregar hábito

3. **Empty State**
   - Se muestra cuando no hay hábitos

**Adapter (`HabitosAdapter`)**:

- **ViewHolder**: Muestra nombre, descripción, switch de completado, botones editar/eliminar
- **Callbacks**:
  - `onCompletadoChanged`: Marca como completado
  - `onEliminar`: Muestra diálogo de confirmación
  - `onEditar`: Abre diálogo de edición

**Diálogos**:

1. **mostrarDialogoHabito(habito?)**
   - Si `habito == null` → Agregar
   - Si `habito != null` → Editar
   - Validación: nombre requerido
   - Llama a ViewModel según operación

2. **mostrarDialogoEliminar(habito)**
   - Confirmación antes de eliminar
   - Muestra nombre del hábito

**Observadores**:

1. **habitos**
   - Actualiza adapter con `submitList()`
   - Muestra/oculta empty state

2. **error**
   - Muestra Toast con error

**Interacciones**:
- Usa `HabitoViewModel` para todas las operaciones
- Usa `AuthViewModel` para obtener userId
- Inicia observación en tiempo real al crear
- Detiene observación en `onDestroyView()`

---

### 3.6 Utilidades

#### `Constants.kt`
**Ubicación**: `util/Constants.kt`

**¿Qué hace?**
Centraliza todas las constantes de la aplicación.

**Categorías**:

1. **Firestore**: Nombres de collections y campos
2. **Time**: Constantes de tiempo (milisegundos en un día)
3. **Validation**: Reglas de validación (longitud mínima password)
4. **LogTags**: Tags para logging consistente
5. **ErrorMessages**: Mensajes de error estandarizados
6. **SuccessMessages**: Mensajes de éxito
7. **UIMessages**: Mensajes para el usuario

**Ventajas**:
- Evita strings mágicos
- Facilita mantenimiento
- Consistencia en toda la app

---

#### `BalanceUApplication.kt`
**Ubicación**: `BalanceUApplication.kt` (raíz)

**¿Qué hace?**
Clase Application personalizada.

**Funcionalidad**:
- Se ejecuta al iniciar la app
- Firebase se inicializa automáticamente si `google-services.json` está presente
- Logging de inicialización

**Configuración**:
- Declarada en `AndroidManifest.xml` como `android:name`

---

## 4. DIAGRAMAS

### 4.1 Diagrama de Arquitectura MVVM

```
┌─────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                     │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ AuthActivity │  │ MainActivity │  │  Fragments    │     │
│  │              │  │              │  │              │     │
│  │ - UI Events  │  │ - Navigation │  │ - UI Events  │     │
│  │ - Observes   │  │ - Observes   │  │ - Observes   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                │                  │              │
│         └────────────────┴──────────────────┘              │
│                            │                                │
│                            ▼                                │
│                  ┌──────────────────┐                       │
│                  │   ViewModels     │                       │
│                  │                  │                       │
│                  │ - LiveData      │                       │
│                  │ - Business Logic│                       │
│                  │ - Coroutines    │                       │
│                  └────────┬────────┘                       │
└────────────────────────────┼────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                         DATA LAYER                            │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Repositories │  │ Repositories │  │ Repositories │     │
│  │              │  │              │  │              │     │
│  │ AuthRepo     │  │ HabitoRepo   │  │ EstadoRepo   │     │
│  │              │  │              │  │              │     │
│  │ - CRUD       │  │ - CRUD       │  │ - CRUD       │     │
│  │ - Listeners  │  │ - Listeners  │  │ - Listeners  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                │                  │              │
│         └────────────────┴──────────────────┘              │
│                            │                                │
│                            ▼                                │
│                  ┌──────────────────┐                       │
│                  │    Firebase      │                       │
│                  │                  │                       │
│                  │ - Auth          │                       │
│                  │ - Firestore     │                       │
│                  └──────────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Diagrama de Flujo de Autenticación

```
                    [Usuario abre app]
                            │
                            ▼
                    [AuthActivity]
                            │
                            ▼
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
      [LoginFragment]            [RegisterFragment]
              │                           │
              │ Usuario ingresa           │ Usuario ingresa
              │ credenciales              │ datos
              │                           │
              └───────────┬───────────────┘
                          │
                          ▼
                  [AuthViewModel]
                          │
                          ▼
                  [AuthRepository]
                          │
                          ▼
                  [Firebase Auth]
                          │
                    ┌─────┴─────┐
                    │           │
                    ▼           ▼
              [Éxito]      [Error]
                    │           │
                    │           └───► [Mostrar error]
                    │
                    ▼
            [MainActivity]
                    │
                    ▼
            [Verificar sesión]
                    │
                    ▼
            [InicioFragment]
```

### 4.3 Diagrama de Flujo de Datos (Registro de Estado de Ánimo)

```
[InicioFragment]
    │ Usuario selecciona estado y presiona "Guardar"
    ▼
[EstadoAnimoViewModel.registrarEstadoAnimo()]
    │ (Coroutine)
    ▼
[EstadoAnimoRepository.agregarEstadoAnimo()]
    │
    ▼
[Firestore: collection("estadosAnimo").add()]
    │
    ├──► [Éxito] ──► [ViewModel._operacionExitosa.postValue(true)]
    │                      │
    │                      ▼
    │              [InicioFragment observa]
    │                      │
    │                      ▼
    │              [Mostrar mensaje, limpiar formulario]
    │
    └──► [Error] ──► [ViewModel._error.postValue()]
                          │
                          ▼
                  [InicioFragment muestra error]
```

### 4.4 Diagrama de Observación en Tiempo Real

```
[InicioFragment.onViewCreated()]
    │
    ▼
[InicioViewModel.startObservandoHabitosDelDia()]
    │
    ▼
[HabitoRepository.observarHabitosDelDia()]
    │
    ▼
[Firestore Query con addSnapshotListener]
    │
    │ Listener activo ──────────────┐
    │                                │
    │                                │ [Cambio en Firestore]
    │                                │ (Usuario marca hábito completado)
    │                                │
    │                                ▼
    │                        [Firestore ejecuta callback]
    │                                │
    │                                ▼
    │                        [Repository procesa documentos]
    │                                │
    │                                ▼
    │                        [Repository.onUpdate(lista)]
    │                                │
    │                                ▼
    │                        [ViewModel._habitosDelDia.postValue()]
    │                                │
    │                                ▼
    └────────────────────────────────┘
                    │
                    ▼
            [LiveData notifica]
                    │
                    ▼
            [InicioFragment observa]
                    │
                    ▼
            [UI se actualiza automáticamente]
            (Contador: "3/5 hábitos completados")
```

### 4.5 Diagrama de Dependencias entre Componentes

```
┌─────────────────────────────────────────────────────────┐
│                    FRAGMENTS                             │
│  InicioFragment ──┐                                      │
│  HabitosFragment ─┼──► ViewModels ──┐                   │
│  LoginFragment ───┘                  │                   │
│  RegisterFragment                    │                   │
└──────────────────────────────────────┼───────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────┐
│                    VIEWMODELS                            │
│  InicioViewModel ──┐                                     │
│  HabitoViewModel ─┼──► Repositories ──┐                │
│  EstadoAnimoVM ────┤                   │                │
│  AuthViewModel ───┘                   │                │
└───────────────────────────────────────┼────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────┐
│                    REPOSITORIES                          │
│  AuthRepository ──┐                                     │
│  HabitoRepository ┼──► Firebase ──┐                   │
│  EstadoAnimoRepo ─┤                 │                   │
│  FraseRepository ─┘                 │                   │
└──────────────────────────────────────┼──────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────┐
│                    FIREBASE                              │
│  Firebase Authentication                                 │
│  Firestore Database                                     │
└─────────────────────────────────────────────────────────┘
```

### 4.6 Diagrama de Navegación

```
                    [AuthActivity]
                    (Login/Register)
                           │
                           │ Éxito
                           ▼
                    [MainActivity]
                           │
                           │ Bottom Navigation
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
[InicioFragment]  [HabitosFragment]  [Otros Fragments]
        │                  │                  │
        │                  │                  │
        └──────────────────┴──────────────────┘
                           │
                           │ (Todos usan NavController)
                           ▼
                    [Navigation Graph]
```

---

## 5. POSIBLES MEJORAS Y PUNTOS DÉBILES

### 5.1 Puntos Débiles Actuales

#### 🔴 Críticos

1. **Falta de manejo de estados de carga**
   - No hay indicadores de carga consistentes
   - El usuario no sabe si la app está procesando algo
   - **Solución**: Agregar `loading: LiveData<Boolean>` en ViewModels

2. **Falta de validación de email**
   - No se valida formato de email en registro/login
   - **Solución**: Agregar validación con regex o `Patterns.EMAIL_ADDRESS`

3. **No hay manejo de conexión a internet**
   - La app falla silenciosamente sin internet
   - **Solución**: Agregar `NetworkCallback` y mostrar mensajes apropiados

4. **Memory leaks potenciales**
   - Los listeners de Firestore pueden no detenerse en algunos casos
   - **Solución**: Asegurar que todos los listeners se detengan en `onCleared()`

#### 🟡 Importantes

5. **Código duplicado en cálculo de límites del día**
   - `InicioViewModel` y `EstadoAnimoViewModel` tienen la misma lógica
   - **Solución**: Extraer a función de utilidad o extensión

6. **Falta de paginación en listas**
   - Si hay muchos hábitos, se cargan todos a la vez
   - **Solución**: Implementar paginación con Firestore

7. **No hay caché local**
   - Si no hay internet, no se puede usar la app
   - **Solución**: Implementar Room Database como caché

8. **Mensajes de error genéricos**
   - Algunos errores no son claros para el usuario
   - **Solución**: Mapear excepciones de Firebase a mensajes amigables

9. **Falta de pruebas unitarias**
   - No hay tests para ViewModels o Repositories
   - **Solución**: Agregar tests con JUnit y Mockito

10. **Validación de datos inconsistente**
    - Algunas validaciones están en Fragment, otras en ViewModel
    - **Solución**: Centralizar validaciones en ViewModel

#### 🟢 Menores

11. **Código comentado en AuthRepository**
    - Hay código comentado para guardar usuario en Firestore
    - **Solución**: Eliminar o implementar completamente

12. **Parámetros no utilizados**
    - `usuarioId` en `marcarCompletado()` y `eliminarHabito()` no se usa
    - **Solución**: Eliminar parámetros o usarlos para validación

13. **Falta de internacionalización (i18n)**
    - Todos los textos están hardcodeados en español
    - **Solución**: Mover a `strings.xml` y agregar soporte multiidioma

14. **No hay logging estructurado**
    - Los logs son inconsistentes
    - **Solución**: Usar librería de logging (Timber) o mejorar estructura

### 5.2 Mejoras Sugeridas

#### 🚀 Funcionalidades

1. **Sistema de recordatorios**
   - Notificaciones para completar hábitos
   - Recordatorios para registrar estado de ánimo

2. **Gráficas y estadísticas**
   - Gráfica de progreso de hábitos
   - Gráfica de estados de ánimo a lo largo del tiempo
   - Estadísticas semanales/mensuales

3. **Hábitos recurrentes**
   - Hábitos que se repiten diariamente
   - Hábitos semanales o mensuales

4. **Metas y objetivos**
   - Establecer metas para hábitos
   - Seguimiento de progreso hacia metas

5. **Compartir logros**
   - Compartir progreso en redes sociales
   - Exportar datos

6. **Modo offline mejorado**
   - Sincronización cuando vuelve la conexión
   - Indicador de estado de sincronización

#### 🏗️ Arquitectura

7. **Inyección de dependencias**
   - Usar Hilt o Koin para DI
   - Facilita testing y mantenimiento

8. **Separar lógica de UI**
   - Extraer lógica de formateo a clases separadas
   - Usar Data Binding o View Binding más extensivamente

9. **Repository pattern mejorado**
   - Interfaz para repositories (facilita testing)
   - Implementación local y remota

10. **Use Cases (Clean Architecture)**
    - Extraer lógica de negocio a casos de uso
    - ViewModels más delgados

#### 🎨 UI/UX

11. **Animaciones**
    - Transiciones suaves entre pantallas
    - Animaciones al completar hábitos

12. **Temas**
    - Modo oscuro
    - Temas personalizables

13. **Onboarding**
    - Tutorial para nuevos usuarios
    - Explicación de funcionalidades

14. **Accesibilidad**
    - Soporte para TalkBack
    - Contraste mejorado
    - Tamaños de fuente ajustables

#### 🔒 Seguridad

15. **Validación de sesión periódica**
    - Verificar que la sesión sigue válida
    - Renovar tokens si es necesario

16. **Encriptación de datos sensibles**
    - Si se guardan datos localmente
    - Encriptar notas personales

17. **Rate limiting**
    - Limitar intentos de login fallidos
    - Prevenir ataques de fuerza bruta

#### 📊 Performance

18. **Lazy loading de imágenes**
    - Si se agregan imágenes en el futuro
    - Usar Glide o Coil

19. **Optimización de queries**
    - Índices en Firestore para queries complejas
    - Limitar cantidad de datos cargados

20. **Caché de imágenes y datos**
    - Cachear frases motivacionales
    - Cachear estados de ánimo recientes

---

## 6. RESUMEN EJECUTIVO

### 6.1 Fortalezas del Código

✅ **Arquitectura clara**: MVVM bien implementado  
✅ **Separación de responsabilidades**: Cada capa tiene su función  
✅ **Uso de LiveData**: Reactividad bien implementada  
✅ **Listeners en tiempo real**: Sincronización automática  
✅ **Manejo de errores**: Uso de Result para operaciones  
✅ **Código documentado**: KDoc en clases y métodos  
✅ **Constantes centralizadas**: Fácil mantenimiento  

### 6.2 Áreas de Mejora Prioritarias

1. **Manejo de estados de carga** (Crítico)
2. **Validación de email** (Crítico)
3. **Manejo de conexión** (Crítico)
4. **Tests unitarios** (Importante)
5. **Caché local** (Importante)

### 6.3 Conclusión

El proyecto está bien estructurado y sigue buenas prácticas de Android. La arquitectura MVVM está correctamente implementada, y el uso de Firebase permite sincronización en tiempo real. Las principales mejoras deberían enfocarse en robustez (manejo de errores, validaciones), testing y experiencia de usuario (estados de carga, feedback visual).

---

**Documentación generada el**: $(date)  
**Versión de la app**: 1.0  
**Última actualización**: Después de refactorización completa

