-- Fonte: docs/02-regras-de-negocio.md, seção 4 (RN01/RN02).
INSERT INTO app.perfis_mobilidade (codigo, nome) VALUES
    ('cadeirante', 'Cadeirante'),
    ('muleta_ou_outro_auxilio', 'Muleta ou outro auxílio'),
    ('mobilidade_reduzida', 'Mobilidade reduzida (outros casos)');

INSERT INTO app.tipos_barreira (codigo, nome) VALUES
    ('escada_sem_alternativa', 'Escada sem alternativa'),
    ('ausencia_rampa', 'Ausência de rampa'),
    ('rampa_inclinacao_inadequada', 'Rampa com inclinação inadequada'),
    ('calcada_estreita', 'Calçada estreita'),
    ('calcada_danificada_irregular', 'Calçada danificada ou irregular'),
    ('obstaculo_calcada', 'Obstáculo na calçada'),
    ('elevador_indisponivel', 'Elevador indisponível'),
    ('entrada_inadequada', 'Entrada inadequada (degrau, porta estreita)'),
    ('banheiro_nao_acessivel', 'Banheiro não acessível'),
    ('ausencia_piso_tatil', 'Ausência de piso tátil');

WITH matriz (barreira, cadeirante, auxilio, outros) AS (
    VALUES
        ('escada_sem_alternativa',     'impeditiva', 'atencao',     'atencao'),
        ('ausencia_rampa',             'impeditiva', 'atencao',     'atencao'),
        ('rampa_inclinacao_inadequada','impeditiva', 'atencao',     'atencao'),
        ('calcada_estreita',           'impeditiva', 'atencao',     'irrelevante'),
        ('calcada_danificada_irregular','atencao',   'impeditiva',  'atencao'),
        ('obstaculo_calcada',          'impeditiva', 'atencao',     'atencao'),
        ('elevador_indisponivel',      'impeditiva', 'atencao',     'atencao'),
        ('entrada_inadequada',        'impeditiva', 'atencao',     'atencao'),
        ('banheiro_nao_acessivel',     'impeditiva', 'atencao',     'irrelevante'),
        ('ausencia_piso_tatil',        'irrelevante','irrelevante', 'irrelevante')
)
INSERT INTO app.classificacoes_barreira (perfil_mobilidade_id, tipo_barreira_id, classificacao)
SELECT p.id, b.id, c.classificacao
FROM matriz m
CROSS JOIN LATERAL (
    VALUES
        ('cadeirante', m.cadeirante),
        ('muleta_ou_outro_auxilio', m.auxilio),
        ('mobilidade_reduzida', m.outros)
) AS c(perfil, classificacao)
JOIN app.perfis_mobilidade p ON p.codigo = c.perfil
JOIN app.tipos_barreira b ON b.codigo = m.barreira;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM app.perfis_mobilidade p
        CROSS JOIN app.tipos_barreira b
        LEFT JOIN app.classificacoes_barreira c
            ON c.perfil_mobilidade_id = p.id AND c.tipo_barreira_id = b.id
        WHERE p.ativo AND b.ativo AND c.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Matriz incompleta: toda combinação ativa precisa de classificação';
    END IF;
END;
$$;
