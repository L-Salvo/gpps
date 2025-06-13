-- Agregar columna ruta_archivo a la tabla actividad si no existe
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'actividad' 
        AND column_name = 'ruta_archivo'
    ) THEN
        ALTER TABLE actividad ADD COLUMN ruta_archivo VARCHAR(255);
    END IF;
END $$; 