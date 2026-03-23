# План реализации: Android Chat App

## Обзор

Реализация Android-приложения на Kotlin + Jetpack Compose с Supabase PostgreSQL.
Архитектура: MVVM + Repository pattern. Навигация: Bottom Navigation Bar (3 вкладки).
Аутентификация — вручную через таблицу `users` с хешированием SHA-256.

## Задачи

- [x] 1. Настройка проекта: зависимости и BuildConfig
  - Добавить в `gradle/libs.versions.toml` версии и библиотеки: supabase-postgrest, ktor-android, navigation-compose, lifecycle-viewmodel-compose, datastore-preferences, coil-compose, kotest-runner-junit5, kotest-property, mockk, kotlinx-coroutines-test
  - Добавить в `app/build.gradle.kts`: все зависимости из libs, `buildFeatures { buildConfig = true }`, чтение `local.properties` и два `buildConfigField` для `SUPABASE_URL` и `SUPABASE_ANON_KEY`
  - Добавить в `AndroidManifest.xml` разрешение `SEND_SMS` и `READ_MEDIA_IMAGES`
  - _Requirements: 9.3, 4.4, 5.2_

- [x] 2. Инициализация Supabase и Application-класс
  - [x] 2.1 Создать `App.kt` (наследник `Application`) с синглтоном `SupabaseClient` через `createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) { install(Postgrest) }`
  - Зарегистрировать `App` в `AndroidManifest.xml` через `android:name`
  - _Requirements: 9.3, 9.4_

- [x] 3. Доменные модели и перечисления
  - [x] 3.1 Создать файл `model/User.kt` с `data class User(id, login, nickname, phone, vkId, avatarUrl, requestsCount, createdAt)`
  - [x] 3.2 Создать файл `model/AppTheme.kt` с `enum class AppTheme { DARK, LIGHT, SYSTEM }`
  - [x] 3.3 Создать файл `model/AiAssistant.kt` с `enum class AiAssistant(val displayName: String)` — ChatGPT, DeepSeek, Qwen, Perplexity
  - [x] 3.4 Создать файл `model/AuthUiState.kt` с `sealed class AuthUiState { Idle, Loading, Error(message), Success }`
  - _Requirements: 1.1, 2.1, 4.1, 9.1_

- [x] 4. SessionManager
  - [x] 4.1 Создать интерфейс `session/SessionManager.kt` и реализацию `SharedPreferencesSessionManager` — методы `saveUserId`, `getUserId`, `clearSession`, `isLoggedIn`
  - [ ]* 4.2 Написать unit-тест `SessionManagerTest` — сохранение, чтение, очистка сессии с FakeSharedPreferences
    - _Requirements: 1.5, 2.4, 3.2_

- [x] 5. PasswordHasher
  - [x] 5.1 Создать `util/PasswordHasher.kt` — `object` с методами `hash(password: String): String` (SHA-256, hex) и `verify(password, hash): Boolean`
  - [ ]* 5.2 Написать property-тест `PasswordHasherPropertyTest`
    - **Property 1: Хеширование пароля — round-trip**
    - **Validates: Requirements 1.6, 1.2, 2.2, 7.4**
  - [ ]* 5.3 Написать unit-тест `PasswordHasherTest` — хеш конкретных строк, верификация, пустая строка
    - _Requirements: 1.6, 2.2, 7.4_

- [x] 6. UserRepository
  - [x] 6.1 Создать интерфейс `repository/UserRepository.kt` с методами: `register`, `login`, `getUserById`, `updateUser`, `incrementRequestsCount`
  - [x] 6.2 Создать `repository/SupabaseUserRepository.kt` — реализация через `supabase.postgrest["users"]`; при SELECT/UPDATE добавлять заголовок `x-user-id` из `SessionManager`; `register` — insert, `login` — select по phone + проверка хеша, `updateUser` — update по id, `incrementRequestsCount` — rpc или update с `requests_count + 1`
  - [ ]* 6.3 Написать property-тест `UserRepositoryPropertyTest`
    - **Property 2: Уникальность логина при регистрации и обновлении**
    - **Validates: Requirements 1.3, 7.2, 7.3**
  - [ ]* 6.4 Написать property-тест для Property 3
    - **Property 3: Уникальность номера телефона при регистрации**
    - **Validates: Requirements 1.4**
  - [ ]* 6.5 Написать property-тест для Property 8
    - **Property 8: Неверные учётные данные при входе возвращают ошибку**
    - **Validates: Requirements 2.3**
  - [ ]* 6.6 Написать property-тест для Property 11
    - **Property 11: Счётчик запросов увеличивается ровно на 1**
    - **Validates: Requirements 4.8**
  - _Requirements: 1.2, 1.3, 1.4, 2.2, 2.3, 4.8, 7.2, 9.3_

- [x] 7. SettingsRepository
  - [x] 7.1 Создать интерфейс `repository/SettingsRepository.kt` и реализацию `DataStoreSettingsRepository` — `Flow<AppTheme>`, `Flow<AiAssistant>`, `Flow<String>` (smsPhone); методы `setTheme`, `setDefaultAi`, `setSmsPhone`; дефолты: SYSTEM, CHATGPT, "89155399434"
  - [ ]* 7.2 Написать property-тест `SettingsRepositoryPropertyTest`
    - **Property 12: Настройки сохраняются и читаются корректно — round-trip**
    - **Validates: Requirements 6.2, 6.4, 6.6**
  - [ ]* 7.3 Написать unit-тест `SettingsRepositoryTest` — дефолтные значения при первом запуске
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 8. SmsLauncher
  - [x] 8.1 Создать `util/SmsLauncher.kt` — `object` с методом `launch(context, phone, text)`: `Intent(ACTION_SENDTO, Uri.parse("smsto:$phone"))` + `EXTRA_SMS_BODY = text`, запуск через `context.startActivity`
  - [ ]* 8.2 Написать property-тест `SmsLauncherPropertyTest`
    - **Property 10: SmsLauncher формирует корректный Intent**
    - **Validates: Requirements 4.4, 4.7**
  - [ ]* 8.3 Написать unit-тест `SmsLauncherTest` — корректность URI и тела Intent
    - _Requirements: 4.4, 4.7_

- [x] 9. Checkpoint — базовый слой данных
  - Убедиться, что все тесты слоя данных проходят. Уточнить у пользователя, если есть вопросы.

- [x] 10. AuthViewModel и валидация форм
  - [x] 10.1 Создать `util/Validators.kt` с функциями `validateRegistrationForm(login, password, nickname, phone)` и `validateLoginForm(phone, password)` — возвращают `Result<Unit>`, проверяют на blank
  - [x] 10.2 Создать `viewmodel/AuthViewModel.kt` — `StateFlow<AuthUiState>`; методы `register(login, password, nickname, phone)` и `login(phone, password)`: валидация → `PasswordHasher.hash` → `UserRepository` → `SessionManager.saveUserId` → `AuthUiState.Success`; обработка ошибок Supabase (unique violation → понятное сообщение)
  - [ ]* 10.3 Написать property-тест `ValidationPropertyTest`
    - **Property 7: Валидация пустых обязательных полей**
    - **Validates: Requirements 1.7, 4.5, 7.6**
  - [ ]* 10.4 Написать unit-тест `AuthViewModelTest` — переходы Idle → Loading → Success/Error
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4_

- [x] 11. AuthScreen
  - [x] 11.1 Создать `ui/auth/AuthScreen.kt` — два режима (вход / регистрация) переключаются через `TabRow` или кнопку; поля ввода с `OutlinedTextField`; кнопки "Войти" / "Зарегистрироваться"; отображение ошибок через `Snackbar`; при `AuthUiState.Success` — вызов `onSuccess()` callback
  - _Requirements: 1.1, 1.3, 1.4, 1.7, 2.1, 2.3_

- [x] 12. Навигация и MainActivity
  - [x] 12.1 Создать `navigation/AppNavigation.kt` — `NavHost` с двумя маршрутами: `"auth"` и `"main"`; стартовый маршрут определяется через `SessionManager.isLoggedIn()`
  - [x] 12.2 Создать `navigation/MainScreen.kt` — `Scaffold` с `NavigationBar` (3 вкладки: Чаты, Профиль, Скоро); вложенный `NavHost` для переключения вкладок
  - [x] 12.3 Обновить `MainActivity.kt` — установить `AppNavigation` как контент, передать `SessionManager`
  - [ ]* 12.4 Написать property-тест `NavigationPropertyTest`
    - **Property 6: Навигация определяется состоянием сессии**
    - **Validates: Requirements 2.5, 2.6, 3.1**
  - _Requirements: 2.5, 2.6, 3.1, 8.2_

- [x] 13. Тема приложения
  - [x] 13.1 Обновить `ui/theme/Theme.kt` — принимать `AppTheme` параметр; выбирать `darkColorScheme` / `lightColorScheme` / системную тему; `MaterialTheme` оборачивает весь контент
  - [x] 13.2 Пробросить `themeFlow` из `SettingsRepository` в `MainActivity` через `collectAsState()`; передавать текущую тему в `AppTheme`
  - _Requirements: 6.1, 6.2_

- [x] 14. PlaceholderScreen
  - [x] 14.1 Создать `ui/placeholder/PlaceholderScreen.kt` — `Box` по центру с `Text("Скоро здесь появится что-то интересное")`
  - _Requirements: 8.1, 8.2_

- [x] 15. ChatViewModel и ChatScreen
  - [x] 15.1 Создать `viewmodel/ChatViewModel.kt` — `StateFlow<AiAssistant>` (selectedAi, дефолт из `SettingsRepository`), `StateFlow<String>` (inputText); методы `selectAi(ai)`, `updateInput(text)`, `sendRequest(context)` — вызывает `SmsLauncher.launch`, затем `UserRepository.incrementRequestsCount`
  - [x] 15.2 Создать `ui/chat/ChatScreen.kt` — 4 кнопки AI с визуальным выделением выбранной (`FilledTonalButton` / `OutlinedButton`); `OutlinedTextField` для ввода; кнопка "Отправить" (disabled если inputText.isBlank()); подписка на `selectedAi` и `inputText` через `collectAsState()`
  - [ ]* 15.3 Написать property-тест `ChatViewModelPropertyTest`
    - **Property 9: Выбор AI обновляет состояние ViewModel**
    - **Validates: Requirements 4.2**
  - [ ]* 15.4 Написать unit-тест `ChatViewModelTest` — блокировка кнопки при пустом тексте, AI по умолчанию
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

- [x] 16. Checkpoint — UI слой (Auth + Chat)
  - Убедиться, что все тесты проходят. Уточнить у пользователя, если есть вопросы.

- [x] 17. ProfileViewModel и ProfileScreen
  - [x] 17.1 Создать `viewmodel/ProfileViewModel.kt` — `StateFlow<User?>` (user), `StateFlow<Uri?>` (avatarUri); методы `loadUser()` через `UserRepository.getUserById`, `updateAvatar(uri: Uri)` — сохраняет URI локально в `StateFlow`
  - [x] 17.2 Создать `ui/profile/ProfileScreen.kt` — аватарка через `AsyncImage` (Coil) с `clickable` для открытия галереи (`rememberLauncherForActivityResult(GetContent)`); никнейм, дата регистрации, счётчик запросов; иконка-кнопка для открытия `SettingsSheet`
  - [ ]* 17.3 Написать property-тест `ProfileViewModelPropertyTest`
    - **Property 13: Аватарка сохраняется локально — round-trip**
    - **Validates: Requirements 5.3**
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 18. SettingsSheet
  - [x] 18.1 Создать `viewmodel/SettingsViewModel.kt` — `StateFlow<AppTheme>`, `StateFlow<AiAssistant>`, `StateFlow<String>` (smsPhone); методы `setTheme`, `setDefaultAi`, `setSmsPhone` делегируют в `SettingsRepository`
  - [x] 18.2 Создать `ui/settings/SettingsSheet.kt` — `ModalBottomSheet` или `ModalDrawer`; три секции: выбор темы (`RadioButton` × 3), выбор AI по умолчанию (`RadioButton` × 4), выбор номера SMS (`RadioButton` × 2); пункт "Безопасность" (открывает `SecuritySheet`); кнопка "Выйти" красным цветом (`MaterialTheme.colorScheme.error`)
  - _Requirements: 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 19. SecuritySheet
  - [x] 19.1 Создать `viewmodel/SecurityViewModel.kt` — `StateFlow<SecurityUiState>`; методы `updateLogin`, `updateNickname`, `updatePassword`, `updateVkId` — валидация на blank → `PasswordHasher.hash` (для пароля) → `UserRepository.updateUser`; обработка ошибки уникальности логина
  - [x] 19.2 Создать `ui/settings/SecuritySheet.kt` — `ModalBottomSheet` с четырьмя `OutlinedTextField` (логин, никнейм, пароль, VK ID); кнопка "Сохранить"; отображение ошибок и успеха через `Snackbar`
  - [ ]* 19.3 Написать property-тест `SecurityViewModelPropertyTest`
    - **Property 4: Сессия сохраняется после успешной аутентификации**
    - **Validates: Requirements 1.5, 2.4**
  - [ ]* 19.4 Написать property-тест для Property 5
    - **Property 5: Выход очищает сессию — round-trip**
    - **Validates: Requirements 3.2**
  - _Requirements: 3.2, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 20. Финальный checkpoint — интеграция
  - Убедиться, что все компоненты связаны: `AppNavigation` использует `SessionManager`, `MainScreen` передаёт `SettingsViewModel` в `SettingsSheet`, `SettingsSheet` открывает `SecuritySheet`, кнопка "Выйти" вызывает `SessionManager.clearSession()` и навигирует на `AuthScreen`. Убедиться, что все тесты проходят. Уточнить у пользователя, если есть вопросы.

## Примечания

- Задачи, помеченные `*`, опциональны и могут быть пропущены для быстрого MVP
- Каждая задача ссылается на конкретные требования для трассируемости
- Property-тесты используют Kotest с минимум 100 итерациями
- Unit-тесты используют JUnit 4 + Mockk + kotlinx-coroutines-test
- `local.properties` с ключами Supabase не коммитится в git
