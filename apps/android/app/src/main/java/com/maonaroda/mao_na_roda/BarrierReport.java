package com.maonaroda.mao_na_roda;

final class BarrierReport {
    final String id;
    final String category;
    final String description;
    final long createdAt;
    final String photoUri;

    BarrierReport(String id, String category, String description, long createdAt, String photoUri) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
        this.photoUri = photoUri;
    }
}
