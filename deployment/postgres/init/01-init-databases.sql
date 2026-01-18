\echo '=========================================='
\echo 'Starting Pictorium database initialization'
\echo '=========================================='


\echo 'Creating users...'

-- Application services
CREATE USER content_user WITH PASSWORD 'content123';
CREATE USER user_service WITH PASSWORD 'user123';
CREATE USER storage_user WITH PASSWORD 'storage123';
CREATE USER chat_user WITH PASSWORD 'chat123';
CREATE USER notification_user WITH PASSWORD 'notification123';

\echo 'Users created successfully!'


\echo 'Creating databases...'

-- Content service database
CREATE DATABASE content_db
    WITH OWNER = content_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

-- User service database
CREATE DATABASE users_db
    WITH OWNER = user_service
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

-- Storage service database
CREATE DATABASE storage_db
    WITH OWNER = storage_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

-- Chat service database
CREATE DATABASE chat_db
    WITH OWNER = chat_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

-- Notification service database
CREATE DATABASE notification_db
    WITH OWNER = notification_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

\echo 'Databases created successfully!'


\echo 'Granting database privileges...'

GRANT ALL PRIVILEGES ON DATABASE content_db TO content_user;
GRANT ALL PRIVILEGES ON DATABASE users_db TO user_service;
GRANT ALL PRIVILEGES ON DATABASE storage_db TO storage_user;
GRANT ALL PRIVILEGES ON DATABASE chat_db TO chat_user;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO notification_user;

-- Content service
\c content_db
GRANT ALL ON SCHEMA public TO content_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO content_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO content_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO content_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO content_user;
\echo 'Content database configured!'

-- User service
\c users_db
GRANT ALL ON SCHEMA public TO user_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO user_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO user_service;
\echo 'Users database configured!'

-- Storage service
\c storage_db
GRANT ALL ON SCHEMA public TO storage_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO storage_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO storage_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO storage_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO storage_user;
\echo 'Storage database configured!'

-- Chat service
\c chat_db
GRANT ALL ON SCHEMA public TO chat_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO chat_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO chat_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO chat_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO chat_user;
\echo 'Chat database configured!'

-- Notification service
\c notification_db
GRANT ALL ON SCHEMA public TO notification_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO notification_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO notification_user;
\echo 'Notification database configured!'


\echo '=========================================='
\echo 'Database initialization completed!'
\echo ''
\echo 'Created databases:'
\echo '  - content_db      (user: content_user)'
\echo '  - users_db        (user: user_service)'
\echo '  - storage_db      (user: storage_user)'
\echo '  - chat_db         (user: chat_user)'
\echo '  - notification_db (user: notification_user)'
\echo '=========================================='