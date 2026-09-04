# Migraciones archivadas

Estos scripts pertenecen a iteraciones anteriores del esquema y se conservan
como referencia historica. Flyway solo carga los archivos directamente dentro
de `db/migration`; por eso esta carpeta no se ejecuta durante el arranque.

No copiar estos archivos de vuelta a `db/migration`: algunos duplican versiones
activas o crean tablas que ya no utiliza el modelo actual.
