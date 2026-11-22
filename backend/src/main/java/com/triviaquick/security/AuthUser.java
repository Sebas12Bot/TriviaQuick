package com.triviaquick.security;

import com.triviaquick.model.Role;

import java.security.Principal;

/**
 * Identidad autenticada, obtenida siempre del JWT validado y de la base de datos.
 * Nunca se construye a partir de identificadores enviados por el cliente.
 */
public record AuthUser(Long id, String email, String displayName, Role role) implements Principal {

    public boolean isTeacher() {
        return role == Role.TEACHER;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
