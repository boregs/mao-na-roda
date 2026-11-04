-- RLS ativado em todas as tabelas de app (já ativo em produção, fora das migrations).
-- Sem políticas, os grupos JDBC não enxergam nem gravam linhas. As políticas abaixo
-- espelham exatamente os grants de V5/V6; a autorização por usuário continua na API,
-- pois a identidade Auth não é propagada pela conexão compartilhada.
ALTER TABLE app.perfis_mobilidade ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.tipos_barreira ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.classificacoes_barreira ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.locais ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.observacoes_acessibilidade ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.relatos ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.relato_fotos ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.datasets_rotas ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.trechos_pedestres ENABLE ROW LEVEL SECURITY;
ALTER TABLE app.evidencias_trecho ENABLE ROW LEVEL SECURITY;

-- Reafirma o grant de V6, ausente na produção aplicada manualmente.
GRANT SELECT ON app.datasets_rotas, app.trechos_pedestres, app.evidencias_trecho TO maonaroda_api;

DO $$
DECLARE
    tabela text;
BEGIN
    FOREACH tabela IN ARRAY ARRAY[
        'perfis_mobilidade', 'tipos_barreira', 'classificacoes_barreira', 'usuarios',
        'locais', 'observacoes_acessibilidade', 'relatos', 'relato_fotos',
        'datasets_rotas', 'trechos_pedestres', 'evidencias_trecho'
    ] LOOP
        EXECUTE format('DROP POLICY IF EXISTS maonaroda_api_select ON app.%I', tabela);
        EXECUTE format(
            'CREATE POLICY maonaroda_api_select ON app.%I FOR SELECT TO maonaroda_api USING (true)',
            tabela);
    END LOOP;

    FOREACH tabela IN ARRAY ARRAY['usuarios', 'relatos', 'relato_fotos'] LOOP
        EXECUTE format('DROP POLICY IF EXISTS maonaroda_api_insert ON app.%I', tabela);
        EXECUTE format(
            'CREATE POLICY maonaroda_api_insert ON app.%I FOR INSERT TO maonaroda_api WITH CHECK (true)',
            tabela);
    END LOOP;

    FOREACH tabela IN ARRAY ARRAY['relatos', 'relato_fotos'] LOOP
        EXECUTE format('DROP POLICY IF EXISTS maonaroda_descarte_select ON app.%I', tabela);
        EXECUTE format(
            'CREATE POLICY maonaroda_descarte_select ON app.%I FOR SELECT TO maonaroda_descarte USING (true)',
            tabela);
        EXECUTE format('DROP POLICY IF EXISTS maonaroda_descarte_delete ON app.%I', tabela);
        EXECUTE format(
            'CREATE POLICY maonaroda_descarte_delete ON app.%I FOR DELETE TO maonaroda_descarte USING (true)',
            tabela);
    END LOOP;
END;
$$;

-- As colunas atualizáveis continuam limitadas pelo grant de V5.
DROP POLICY IF EXISTS maonaroda_api_update ON app.usuarios;
CREATE POLICY maonaroda_api_update ON app.usuarios
    FOR UPDATE TO maonaroda_api USING (true) WITH CHECK (true);
