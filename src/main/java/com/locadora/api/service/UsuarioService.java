package com.locadora.api.service;

import com.locadora.api.model.Emprestimo;
import com.locadora.api.model.Usuario;
import com.locadora.api.repository.EmprestimoRepository;
import com.locadora.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    public boolean quitarDividas(Long usuarioId) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (!usuarioOpt.isPresent()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();

        // Zera dívida do usuário
        usuario.setDivida(BigDecimal.ZERO);
        usuarioRepository.save(usuario);

        // Zera multas de todos os empréstimos do usuário
        List<Emprestimo> emprestimos = emprestimoRepository.findByUsuarioId(usuarioId);

        for (Emprestimo emp : emprestimos) {
            emp.setMulta(0.0);
            emprestimoRepository.save(emp);
        }

        return true;
    }
}
