package com.locadora.api.controller;

import com.locadora.api.model.Usuario;
import com.locadora.api.model.Emprestimo;
import com.locadora.api.repository.UsuarioRepository;
import com.locadora.api.repository.EmprestimoRepository;
import com.locadora.api.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private UsuarioService usuarioService; // ✅ CORREÇÃO — agora existe

    // Listar todos os usuários
    @GetMapping
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    // Buscar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable("id") Long id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);

        if (usuario.isPresent()) {
            return ResponseEntity.ok(usuario.get());
        } else {
            Map<String, String> erro = new HashMap<String, String>();
            erro.put("erro", "Usuário não encontrado");
            return ResponseEntity.status(404).body(erro);
        }
    }

    // Criar usuário (com validação detalhada de campos obrigatórios)
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Usuario usuario) {

        List<String> camposInvalidos = new ArrayList<>();

        if (usuario.getNome() == null || usuario.getNome().trim().isEmpty()) {
            camposInvalidos.add("nome");
        }
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            camposInvalidos.add("email");
        }
        if (usuario.getTelefone() == null || usuario.getTelefone().trim().isEmpty()) {
            camposInvalidos.add("telefone");
        }
        if (usuario.getDivida() == null) {
            camposInvalidos.add("divida");
        } else if (usuario.getDivida().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("erro", "O valor da dívida não pode ser negativo."));
        }

        if (!camposInvalidos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("erro",
                            "Campos obrigatórios ausentes ou inválidos: " + String.join(", ", camposInvalidos)));
        }

        Usuario salvo = usuarioRepository.save(usuario);
        return new ResponseEntity<>(salvo, HttpStatus.CREATED);
    }

    // 🔧 QUITAR TODAS AS DÍVIDAS (via UsuarioService)
    @PostMapping("/{id}/quitar-dividas")
    public ResponseEntity<?> quitarDividas(@PathVariable Long id) {
        boolean quitou = usuarioService.quitarDividas(id);

        if (!quitou) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado ou não possui dívidas.");
        }

        return ResponseEntity.ok("Dívidas quitadas com sucesso!");
    }

    // Atualizar usuário
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable("id") Long id, @RequestBody Usuario usuarioAtualizado) {

        Optional<Usuario> usuarioExistente = usuarioRepository.findById(id);

        if (!usuarioExistente.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado");
        }

        Usuario usuario = usuarioExistente.get();

        if (usuarioAtualizado.getDivida() != null &&
                !usuarioAtualizado.getDivida().equals(usuario.getDivida())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Campo 'divida' não pode ser alterado manualmente. Faça o pagamento e evite multas maiores.");
        }

        usuario.setNome(usuarioAtualizado.getNome());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setTelefone(usuarioAtualizado.getTelefone());

        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Usuário atualizado com sucesso!");
    }

    // Excluir usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable("id") Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (!usuarioOpt.isPresent()) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Usuário não encontrado");
            return ResponseEntity.status(404).body(erro);
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getDivida() != null && usuario.getDivida().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Usuário não pode ser excluído enquanto possuir dívidas pendentes.");
            return ResponseEntity.status(400).body(erro);
        }

        usuarioRepository.deleteById(id);

        String mensagem = "Usuário (" + usuario.getId() + " - " + usuario.getNome() + ") excluído com sucesso.";
        Map<String, String> resposta = new HashMap<>();
        resposta.put("mensagem", mensagem);

        return ResponseEntity.status(200).body(resposta);
    }

    // Quitar dívida única (campo divida do usuário)
    @PostMapping("/{id}/quitar")
    public ResponseEntity<?> quitarDivida(@PathVariable("id") Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (!usuarioOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("erro", "Usuário não encontrado."));
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setDivida(BigDecimal.ZERO);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Collections.singletonMap("mensagem",
                "Dívida quitada com sucesso para o usuário: " + usuario.getNome()));
    }

    // 🔎 LISTAR TODAS AS DÍVIDAS ATIVAS (usando ID do empréstimo)
    @GetMapping("/dividas")
    public ResponseEntity<?> listarDividasAtivas() {

        List<Emprestimo> emprestimosComMulta =
                emprestimoRepository.findByMultaGreaterThan(0.0);

        List<Map<String, Object>> resposta = new ArrayList<>();

        for (Emprestimo e : emprestimosComMulta) {

            Map<String, Object> dados = new HashMap<>();
            dados.put("emprestimoId", e.getId());
            dados.put("usuarioId", e.getUsuario().getId());
            dados.put("nome", e.getUsuario().getNome());
            dados.put("email", e.getUsuario().getEmail());
            dados.put("divida", e.getMulta());
            dados.put("statusEmprestimo", e.getStatus().name());
            dados.put("dataPrevista", e.getDataPrevistaDevolucao());
            dados.put("dataDevolucao", e.getDataDevolucao());

            resposta.add(dados);
        }

        return ResponseEntity.ok(resposta);
    }
}
