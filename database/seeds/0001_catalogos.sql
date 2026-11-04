-- seeds/0001_catalogos.sql
-- Carga inicial dos catálogos da V1.
-- Fonte: docs/02-regras-de-negocio.md, seção 4 (matriz de barreiras por perfil; RN01 e RN02).
-- Rode DEPOIS de migrations/0001_catalogos.sql. Pode ser executado mais de uma vez.

INSERT INTO app.perfis_mobilidade (codigo, nome) VALUES
    ('cadeirante',               'Cadeirante'),
    ('muleta_ou_auxilio',        'Muleta ou outro auxílio'),
    ('mobilidade_reduzida_outros','Mobilidade reduzida (outros casos)')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO app.tipos_barreira (codigo, nome) VALUES
    ('escada_sem_alternativa',        'Escada sem alternativa'),
    ('ausencia_rampa',                'Ausência de rampa'),
    ('rampa_inclinacao_inadequada',   'Rampa com inclinação inadequada'),
    ('calcada_estreita',              'Calçada estreita'),
    ('calcada_danificada',            'Calçada danificada ou irregular'),
    ('obstaculo_calcada',             'Obstáculo na calçada'),
    ('elevador_indisponivel',         'Elevador indisponível'),
    ('entrada_inadequada',            'Entrada inadequada (degrau, porta estreita)'),
    ('banheiro_nao_acessivel',        'Banheiro não acessível'),
    ('ausencia_piso_tatil',           'Ausência de piso tátil')
ON CONFLICT (codigo) DO NOTHING;

-- Matriz: I = impeditiva, A = atencao, N = irrelevante (–)
-- Colunas: barreira, cadeirante, muleta_ou_auxilio, mobilidade_reduzida_outros
INSERT INTO app.classificacoes_barreira (perfil_mobilidade_id, tipo_barreira_id, classificacao)
SELECT p.id, t.id,
       CASE m.valor WHEN 'I' THEN 'impeditiva' WHEN 'A' THEN 'atencao' ELSE 'irrelevante' END
FROM (VALUES
    ('escada_sem_alternativa',      'I','A','A'),
    ('ausencia_rampa',              'I','A','A'),
    ('rampa_inclinacao_inadequada', 'I','A','A'),
    ('calcada_estreita',            'I','A','N'),
    ('calcada_danificada',          'A','I','A'),
    ('obstaculo_calcada',           'I','A','A'),
    ('elevador_indisponivel',       'I','A','A'),
    ('entrada_inadequada',          'I','A','A'),
    ('banheiro_nao_acessivel',      'I','A','N'),
    ('ausencia_piso_tatil',         'N','N','N')
) AS x(barreira, cadeirante, muleta_ou_auxilio, mobilidade_reduzida_outros)
CROSS JOIN LATERAL (VALUES
    ('cadeirante',                x.cadeirante),
    ('muleta_ou_auxilio',         x.muleta_ou_auxilio),
    ('mobilidade_reduzida_outros',x.mobilidade_reduzida_outros)
) AS m(perfil, valor)
JOIN app.perfis_mobilidade p ON p.codigo = m.perfil
JOIN app.tipos_barreira    t ON t.codigo = x.barreira
ON CONFLICT (perfil_mobilidade_id, tipo_barreira_id) DO NOTHING;

-- Verificação (seção 11 do doc): deve retornar 0 linhas, ou seja, nenhuma combinação sem classificação.
SELECT p.codigo AS perfil, t.codigo AS barreira
FROM app.perfis_mobilidade p
CROSS JOIN app.tipos_barreira t
LEFT JOIN app.classificacoes_barreira c
       ON c.perfil_mobilidade_id = p.id AND c.tipo_barreira_id = t.id
WHERE p.ativo AND t.ativo AND c.id IS NULL;
