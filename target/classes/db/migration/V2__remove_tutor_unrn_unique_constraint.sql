-- Eliminar la restricción de unicidad en tutor_unrn_id
ALTER TABLE proyecto DROP CONSTRAINT IF EXISTS proyecto_tutor_unrn_id_key; 