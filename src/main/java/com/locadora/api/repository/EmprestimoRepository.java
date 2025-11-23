package com.locadora.api.repository;

import com.locadora.api.model.Emprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    // 🔹 Verifica se existe empréstimo ativo vinculado a um item específico
    boolean existsByItemId(Long itemId);

    // 🔹 Busca todos os empréstimos de um usuário com determinados status
    List<Emprestimo> findByUsuarioIdAndStatusIn(Long usuarioId, List<String> status);

    // 🔹 Busca todos os empréstimos associados a um item específico
    List<Emprestimo> findByItemId(Long itemId);

    // 🔹 Busca empréstimos de um usuário com status único
    List<Emprestimo> findByUsuarioIdAndStatus(Long usuarioId, String status);

    // 🔹 Busca todos os empréstimos por status enum
    List<Emprestimo> findByStatus(Emprestimo.StatusEmprestimo status);

    // 🔹 Buscar empréstimos com multa > 0 para impedir exclusão/PUT
    List<Emprestimo> findByUsuarioIdAndMultaGreaterThan(Long usuarioId, double valor);

    // 🔹 Buscar empréstimos com multa > 0
    List<Emprestimo> findByMultaGreaterThan(Double multa);

    // 🔹 Correção: mantido apenas UMA versão compatível com o service
    List<Emprestimo> findByUsuarioIdAndMultaGreaterThan(Long usuarioId, Double multa);

    // 🔹 Novo método necessário para o UsuarioService
    List<Emprestimo> findByUsuarioId(Long usuarioId);
}
