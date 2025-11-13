package com.locadora.api.repository;

import com.locadora.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Consultas customizadas podem ser adicionadas aqui
}
