package com.maonaroda.api.storage;

/** Erro sanitizado: nunca inclui credenciais ou resposta remota. */
public class StorageException extends RuntimeException {
    public StorageException(String message) { super(message); }
}
