package com.locadora.api.service;

import com.locadora.api.model.Emprestimo;
import com.locadora.api.model.Emprestimo.StatusEmprestimo;
import com.locadora.api.model.Item;
import com.locadora.api.model.Usuario;
import com.locadora.api.repository.EmprestimoRepository;
import com.locadora.api.repository.ItemRepository;
import com.locadora.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemRepository itemRepository;

    public EmprestimoService(EmprestimoRepository eRepo, UsuarioRepository uRepo, ItemRepository iRepo) {
        this.emprestimoRepository = eRepo;
        this.usuarioRepository = uRepo;
        this.itemRepository = iRepo;
    }

    public List<Emprestimo> listar() {
        return emprestimoRepository.findAll();
    }

    // 🔹 Cadastro de novo empréstimo
    @Transactional
    public Emprestimo emprestar(Long usuarioId, Long itemId, Integer quantidade, LocalDate dataPrevistaDevolucao) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado"));

        // 🔒 Regra de bloqueio por dívida
        if (usuario.getDivida() != null && usuario.getDivida().doubleValue() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário com dívida pendente — empréstimo bloqueado.");
        }

        // 🔸 Verifica estoque disponível
        if (item.getQuantidade() == null || item.getQuantidade() < quantidade) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estoque insuficiente para este empréstimo.");
        }

        // 🔹 Atualiza estoque
        item.setQuantidade(item.getQuantidade() - quantidade);
        itemRepository.save(item);

        Emprestimo emprestimo = Emprestimo.builder()
                .usuario(usuario)
                .item(item)
                .quantidade(quantidade)
                .dataEmprestimo(LocalDate.now())
                .dataPrevistaDevolucao(dataPrevistaDevolucao)
                .status(StatusEmprestimo.ACTIVE)
                .renovacoes(0)
                .multa(0.0)
                .build();

        return emprestimoRepository.save(emprestimo);
    }

    // 🔹 Devolução de item
    @Transactional
    public Emprestimo devolver(Long id) {
        Emprestimo emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        // 🔒 NOVA REGRA: impede devolução duplicada
        if (emprestimo.getStatus() == StatusEmprestimo.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este empréstimo já foi devolvido anteriormente.");
        }

        // 🔒 Caso já exista data de devolução, bloqueia também
        if (emprestimo.getDataDevolucao() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Devolução já registrada anteriormente.");
        }

        Item item = emprestimo.getItem();
        item.setQuantidade(item.getQuantidade() + emprestimo.getQuantidade());
        itemRepository.save(item);

        LocalDate hoje = LocalDate.now();
        emprestimo.setDataDevolucao(hoje);

        // Cálculo de atraso está correto
        long diasAtraso = Math.max(0,
                ChronoUnit.DAYS.between(emprestimo.getDataPrevistaDevolucao(), hoje)
        );

        Usuario usuario = emprestimo.getUsuario();

        if (diasAtraso > 0) {
            double multa = diasAtraso * 2.50;
            emprestimo.setMulta(multa);
            emprestimo.setStatus(StatusEmprestimo.LATE);

            if (usuario.getDivida() == null) {
                usuario.setDivida(BigDecimal.ZERO);
            }
            usuario.setDivida(usuario.getDivida().add(BigDecimal.valueOf(multa)));
            usuarioRepository.save(usuario);
        } else {
            emprestimo.setMulta(0.0);
            emprestimo.setStatus(StatusEmprestimo.RETURNED);
        }

        return emprestimoRepository.save(emprestimo);
    }

    // 🔹 Renovação de empréstimo (máx. 2 renovações)
    @Transactional
    public Emprestimo renovar(Long id, Integer diasExtra) {
        Emprestimo emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        if (emprestimo.getStatus() != StatusEmprestimo.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente empréstimos ativos podem ser renovados.");
        }

        Usuario usuario = emprestimo.getUsuario();

        // 🔒 Bloqueia se houver dívida
        if (usuario.getDivida() != null && usuario.getDivida().doubleValue() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuário com dívida pendente — renovação bloqueada.");
        }

        // 🔒 Limite de renovações
        if (emprestimo.getRenovacoes() >= 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Limite máximo de 2 renovações atingido — devolução obrigatória.");
        }

        emprestimo.setDataPrevistaDevolucao(
                emprestimo.getDataPrevistaDevolucao().plusDays(diasExtra != null ? diasExtra : 7)
        );
        emprestimo.setRenovacoes(emprestimo.getRenovacoes() + 1);

        return emprestimoRepository.save(emprestimo);
    }

    // 🔹 Consulta de dívidas de um usuário
    @Transactional(readOnly = true)
    public List<Emprestimo> consultarDividasUsuario(Long usuarioId) {
        List<Emprestimo> pendentes = new ArrayList<>(emprestimoRepository.findByUsuarioIdAndStatusIn(
                usuarioId,
                Arrays.asList(StatusEmprestimo.ACTIVE.name(), StatusEmprestimo.LATE.name())
        ));

        List<Emprestimo> comMulta = emprestimoRepository.findByUsuarioIdAndMultaGreaterThan(usuarioId, 0.0);

        for (Emprestimo e : comMulta) {
            if (!pendentes.contains(e)) {
                pendentes.add(e);
            }
        }

        return pendentes;
    }

    // 🔹 Consulta de empréstimo específico
    @Transactional(readOnly = true)
    public Emprestimo consultarEmprestimo(Long id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));
    }
}
