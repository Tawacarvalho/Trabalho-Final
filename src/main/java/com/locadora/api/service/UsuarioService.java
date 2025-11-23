package com.locadora.api.service;

import com.locadora.api.model.Usuario;
import com.locadora.api.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Quita a dívida de um usuário.
     * @return true caso tenha quitado, false se o usuário não existir ou não tiver dívida.
     */
    public boolean quitarDividas(Long idUsuario) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);

        if (!usuarioOpt.isPresent()) {
            return false; // usuário não existe
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getDivida() == null || usuario.getDivida().compareTo(BigDecimal.ZERO) == 0) {
            return false; // não há dívida para quitar
        }

        usuario.setDivida(BigDecimal.ZERO);
        usuarioRepository.save(usuario);

        return true;
    }
}
