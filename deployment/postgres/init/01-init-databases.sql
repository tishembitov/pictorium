\echo '=========================================='
\echo 'Starting Pictorium database initialization'
\echo '=========================================='


\echo 'Creating users...'

-- Keycloak
CREATE USER keycloak WITH PASSWORD 'keycloak123';

-- Application services
CREATE USER content_user WITH PASSWORD 'content123';
CREATE USER user_service WITH PASSWORD 'user123';
CREATE USER storage_user WITH PASSWORD 'storage123';
CREATE USER chat_user WITH PASSWORD 'chat123';

\echo 'Users created successfully!'


\echo 'Creating databases...'

-- Keycloak database
CREATE DATABASE keycloak
    WITH OWNER = keycloak
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

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

CREATE DATABASE chat_db
    WITH OWNER = chat_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8';

\echo 'Databases created successfully!'


\echo 'Granting database privileges...'

GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
GRANT ALL PRIVILEGES ON DATABASE content_db TO content_user;
GRANT ALL PRIVILEGES ON DATABASE users_db TO user_service;
GRANT ALL PRIVILEGES ON DATABASE storage_db TO storage_user;
GRANT ALL PRIVILEGES ON DATABASE chat_db TO chat_user;

-- Keycloak
\c keycloak
GRANT ALL ON SCHEMA public TO keycloak;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO keycloak;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO keycloak;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO keycloak;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO keycloak;
\echo 'Keycloak database configured!'

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

\c pictorium

\echo '=========================================='
\echo 'Database initialization completed!'
\echo ''
\echo 'Created databases:'
\echo '  - keycloak    (user: keycloak)'
\echo '  - content_db  (user: content_user)'
\echo '  - users_db    (user: user_service)'
\echo '  - storage_db  (user: storage_user)'
\echo '=========================================='