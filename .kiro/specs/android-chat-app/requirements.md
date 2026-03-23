# Requirements Document

## Introduction

Android-приложение на Kotlin + Jetpack Compose с Supabase в качестве базы данных.
Приложение предоставляет авторизацию через собственную таблицу пользователей (без Supabase Auth),
три вкладки навигации: чаты с AI-ассистентами (через SMS), личный кабинет и заглушку третьей вкладки.

## Glossary

- **App**: Android-приложение на Kotlin + Jetpack Compose
- **User**: зарегистрированный пользователь приложения
- **AuthScreen**: экран входа и регистрации
- **Navigator**: компонент нижней навигации (Bottom Navigation Bar)
- **ChatScreen**: вкладка "Чаты" с выбором AI и отправкой SMS
- **ProfileScreen**: вкладка "Личный кабинет"
- **PlaceholderScreen**: заглушка третьей вкладки
- **SettingsSheet**: меню настроек (Bottom Sheet или Drawer)
- **SecuritySheet**: меню изменения личных данных
- **Database**: Supabase PostgreSQL база данных
- **UserRepository**: слой доступа к данным пользователей
- **SessionManager**: компонент управления сессией (хранение токена/userId локально)
- **PasswordHasher**: компонент хеширования паролей (bcrypt или SHA-256)
- **SmsLauncher**: компонент открытия SMS-приложения с предзаполненным текстом

---

## Requirements

### Requirement 1: Регистрация пользователя

**User Story:** Как новый пользователь, я хочу зарегистрироваться в приложении, чтобы получить доступ к его функциям.

#### Acceptance Criteria

1. THE App SHALL отображать экран регистрации с полями: логин, пароль, никнейм, номер телефона.
2. WHEN пользователь нажимает кнопку "Зарегистрироваться", THE UserRepository SHALL сохранить запись в таблицу `users` в Database с хешированным паролем.
3. IF логин уже существует в Database, THEN THE AuthScreen SHALL отобразить сообщение об ошибке "Логин уже занят".
4. IF номер телефона уже существует в Database, THEN THE AuthScreen SHALL отобразить сообщение об ошибке "Номер телефона уже зарегистрирован".
5. WHEN регистрация успешна, THE SessionManager SHALL сохранить идентификатор пользователя локально и THE App SHALL перейти на экран с Navigator.
6. THE PasswordHasher SHALL хешировать пароль перед сохранением в Database.
7. IF любое из полей регистрации пустое, THEN THE AuthScreen SHALL отобразить сообщение об ошибке валидации и заблокировать отправку формы.

---

### Requirement 2: Авторизация пользователя

**User Story:** Как зарегистрированный пользователь, я хочу войти по номеру телефона и паролю, чтобы получить доступ к приложению.

#### Acceptance Criteria

1. THE App SHALL отображать экран входа с полями: номер телефона, пароль.
2. WHEN пользователь нажимает кнопку "Войти", THE UserRepository SHALL найти запись в таблице `users` по номеру телефона и сравнить хеш пароля.
3. IF номер телефона не найден или пароль не совпадает, THEN THE AuthScreen SHALL отобразить сообщение "Неверный номер телефона или пароль".
4. WHEN авторизация успешна, THE SessionManager SHALL сохранить идентификатор пользователя локально и THE App SHALL перейти на экран с Navigator.
5. WHILE SessionManager содержит действующий идентификатор пользователя, THE App SHALL пропускать AuthScreen и сразу открывать экран с Navigator.
6. IF приложение открыто без сохранённой сессии, THEN THE App SHALL отобразить AuthScreen.

---

### Requirement 3: Защита доступа

**User Story:** Как владелец продукта, я хочу, чтобы без авторизации пользователь не мог попасть в приложение, чтобы защитить данные.

#### Acceptance Criteria

1. WHILE SessionManager не содержит идентификатора пользователя, THE Navigator SHALL быть недоступен.
2. WHEN пользователь нажимает кнопку "Выйти", THE SessionManager SHALL удалить сохранённый идентификатор и THE App SHALL перейти на AuthScreen.

---

### Requirement 4: Вкладка "Чаты" — выбор AI и отправка SMS

**User Story:** Как пользователь, я хочу выбрать AI-ассистента, ввести запрос и отправить его через SMS, чтобы получить ответ.

#### Acceptance Criteria

1. THE ChatScreen SHALL отображать четыре кнопки выбора AI: ChatGPT, DeepSeek, Qwen, Perplexity.
2. WHEN пользователь выбирает AI, THE ChatScreen SHALL визуально выделить выбранную кнопку.
3. THE ChatScreen SHALL содержать поле ввода текста для запроса пользователя.
4. WHEN пользователь нажимает кнопку "Отправить", THE SmsLauncher SHALL открыть системное SMS-приложение с предзаполненным номером телефона и текстом запроса.
5. IF поле ввода текста пустое, THEN THE ChatScreen SHALL заблокировать кнопку "Отправить".
6. IF AI не выбран, THEN THE ChatScreen SHALL использовать AI по умолчанию из настроек пользователя.
7. WHEN SmsLauncher открывает SMS-приложение, THE App SHALL использовать номер телефона, выбранный в настройках (один из двух: 89155399434 или 89003578107).
8. WHEN пользователь возвращается из SMS-приложения, THE UserRepository SHALL увеличить счётчик `requests_count` пользователя в Database на 1.

---

### Requirement 5: Вкладка "Личный кабинет"

**User Story:** Как пользователь, я хочу видеть свой профиль и управлять настройками, чтобы персонализировать приложение.

#### Acceptance Criteria

1. THE ProfileScreen SHALL отображать аватарку пользователя, никнейм, дату регистрации и количество отправленных запросов.
2. WHEN пользователь нажимает на аватарку, THE App SHALL открыть системный выбор изображения из галереи.
3. WHEN пользователь выбирает изображение из галереи, THE ProfileScreen SHALL отобразить выбранное изображение в качестве аватарки и сохранить его локально.
4. THE ProfileScreen SHALL содержать кнопку или иконку для открытия SettingsSheet.
5. THE SettingsSheet SHALL содержать три пункта: "Настройки", "Безопасность", "Выйти".
6. THE кнопка "Выйти" в SettingsSheet SHALL быть выделена красным цветом.

---

### Requirement 6: Настройки приложения

**User Story:** Как пользователь, я хочу настраивать тему, AI по умолчанию и номер для SMS, чтобы приложение соответствовало моим предпочтениям.

#### Acceptance Criteria

1. THE SettingsSheet SHALL отображать выбор темы: тёмная, светлая, системная.
2. WHEN пользователь выбирает тему, THE App SHALL применить выбранную тему немедленно и сохранить выбор локально.
3. THE SettingsSheet SHALL отображать выбор AI по умолчанию: ChatGPT, DeepSeek, Qwen, Perplexity.
4. WHEN пользователь выбирает AI по умолчанию, THE App SHALL сохранить выбор локально.
5. THE SettingsSheet SHALL отображать выбор номера телефона для SMS: 8 915 539 94 34 или 8 900 357 81 07.
6. WHEN пользователь выбирает номер телефона, THE App SHALL сохранить выбор локально.

---

### Requirement 7: Безопасность — изменение личных данных

**User Story:** Как пользователь, я хочу изменить свои данные (логин, никнейм, пароль, VK ID), чтобы поддерживать актуальность профиля.

#### Acceptance Criteria

1. THE SecuritySheet SHALL отображать поля для изменения: логин, никнейм, пароль, VK ID.
2. WHEN пользователь изменяет логин и подтверждает, THE UserRepository SHALL проверить уникальность нового логина в Database.
3. IF новый логин уже занят, THEN THE SecuritySheet SHALL отобразить сообщение об ошибке "Логин уже занят".
4. WHEN пользователь изменяет пароль, THE PasswordHasher SHALL хешировать новый пароль перед сохранением в Database.
5. WHEN изменения сохранены успешно, THE SecuritySheet SHALL отобразить сообщение об успехе.
6. IF поле логина или никнейма пустое при сохранении, THEN THE SecuritySheet SHALL отобразить сообщение об ошибке валидации.

---

### Requirement 8: Третья вкладка (заглушка)

**User Story:** Как разработчик, я хочу иметь заглушку для третьей вкладки, чтобы навигация была полной.

#### Acceptance Criteria

1. THE PlaceholderScreen SHALL отображать текст-заглушку "Скоро здесь появится что-то интересное".
2. THE Navigator SHALL содержать три вкладки: "Чаты", "Профиль", PlaceholderScreen.

---

### Requirement 9: Структура базы данных Supabase

**User Story:** Как разработчик, я хочу иметь чёткую схему таблиц в Supabase, чтобы корректно хранить данные пользователей.

#### Acceptance Criteria

1. THE Database SHALL содержать таблицу `users` со столбцами: `id` (uuid, primary key), `login` (text, unique, not null), `password_hash` (text, not null), `nickname` (text, not null), `phone` (text, unique, not null), `vk_id` (text, nullable), `avatar_url` (text, nullable), `requests_count` (integer, default 0), `created_at` (timestamptz, default now()).
2. THE Database SHALL применять Row Level Security (RLS) на таблицу `users` так, чтобы пользователь мог читать и обновлять только свою строку по `id`.
3. THE UserRepository SHALL использовать Supabase anon key для выполнения запросов к Database.
4. THE Database SHALL разрешать вставку новых строк в `users` без аутентификации Supabase Auth (через anon key с соответствующей RLS-политикой).
