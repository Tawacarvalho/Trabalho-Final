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

    // 🔹 Busca empréstimos de um usuário com status único (se necessário)
    List<Emprestimo> findByUsuarioIdAndStatus(Long usuarioId, String status);

    // 🔹 Busca todos os empréstimos ativos (caso exista controle de status)
    List<Emprestimo> findByStatus(Emprestimo.StatusEmprestimo status);

    // 🔹 Novo método — busca empréstimos onde a multa é maior que um valor
    //    (usado para impedir exclusão ou PUT quando há dívidas)
    List<Emprestimo> findByUsuarioIdAndMultaGreaterThan(Long usuarioId, double valor);
}
