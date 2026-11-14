# Base de pruebas de integración: biolearn_test

Las pruebas de integración (`*IntegracionTest`) se ejecutan contra una copia **solo de la estructura**
de `biolearn_db`, sin datos. Cada prueba crea sus propios registros y los deshace al terminar.

Créala (o recréala después de cada migración nueva) desde la consola de Windows (cmd), en la carpeta
`bin` de MySQL (por ejemplo `C:\Program Files\MySQL\MySQL Server 8.0\bin`):

```
mysql -u root -p -e "DROP DATABASE IF EXISTS biolearn_test; CREATE DATABASE biolearn_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysqldump -u root -p --no-data --routines --triggers biolearn_db > biolearn_estructura.sql
mysql -u root -p biolearn_test < biolearn_estructura.sql
```

Las pruebas usan el mismo usuario y contraseña de `application.properties`; solo cambian la base a
`biolearn_test`. Si la base no existe, las pruebas de integración se **omiten** (no fallan).

## Permisos del usuario del backend

Si `application.properties` usa un usuario propio (por ejemplo `biolearn_user`) en lugar de `root`, ese usuario
necesita permisos sobre `biolearn_test`. Ejecuta, conectado como `root`:

```sql
GRANT ALL PRIVILEGES ON biolearn_test.* TO 'biolearn_user'@'localhost';
FLUSH PRIVILEGES;
```

(Cambia `'localhost'` por `'%'` si el usuario está definido así: `SELECT user, host FROM mysql.user;`.)
Si no tiene permisos, las pruebas de integración se omiten y la consola muestra el motivo.
