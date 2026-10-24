package com.maonaroda.api.rotas;

public record RotaBase(long distanciaMetros, double duracaoSegundos, String polylineCodificada,
                       String provedor, String modo, boolean acessibilidadeAvaliada, String aviso) {}
