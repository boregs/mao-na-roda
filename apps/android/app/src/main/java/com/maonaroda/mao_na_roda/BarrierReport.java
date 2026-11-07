package com.maonaroda.mao_na_roda;

import com.maonaroda.mao_na_roda.data.DadosRotas.Ponto;

final class BarrierReport {
    final String id;
    final String category;
    final String description;
    final long createdAt;
    final String photoUri;
    final Ponto ponto;

    BarrierReport(String id, String category, String description, long createdAt, String photoUri, Ponto ponto) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
        this.photoUri = photoUri;
        this.ponto = ponto;
    }
}
