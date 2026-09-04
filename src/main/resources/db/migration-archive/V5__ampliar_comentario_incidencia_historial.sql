-- La entidad IncidenciaHistorial declara comentario con length = 2000, pero V4
-- creo la columna como VARCHAR(1000). Hibernate no valida longitudes, asi que la
-- discrepancia solo aparecia al guardar un comentario largo. Se amplia la columna
-- para que coincida con la entidad; ampliar nunca trunca datos existentes.

ALTER TABLE incidencia_historial
    MODIFY COLUMN comentario VARCHAR(2000) NULL;
