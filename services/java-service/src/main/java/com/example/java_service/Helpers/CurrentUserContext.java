package com.example.java_service.Helpers;

public final class CurrentUserContext
{
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CurrentUserContext() {}

    public static void set(String idUser) {
        CURRENT.set(idUser);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
