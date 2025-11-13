package com.locadora.api.controller;

import com.locadora.api.model.Usuario;
import com.locadora.api.repository.UsuarioRepository;
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
import java.util.Optional;


@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

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

        // Se houver campos inválidos, retorna a lista
        if (!camposInvalidos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("erro",
                            "Campos obrigatórios ausentes ou inválidos: " + String.join(", ", camposInvalidos)));
        }

        Usuario salvo = usuarioRepository.save(usuario);
        return new ResponseEntity<>(salvo, HttpStatus.CREATED);
    }

    // Atualizar usuário
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable("id") Long id, @RequestBody Usuario usuarioAtualizado) {
        // Busca o usuário existente
        Optional<Usuario> usuarioExistente = usuarioRepository.findById(id);

        if (!usuarioExistente.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado");
        }

        Usuario usuario = usuarioExistente.get();

        // Impede alteração manual da dívida
        if (usuarioAtualizado.getDivida() != null &&
                !usuarioAtualizado.getDivida().equals(usuario.getDivida())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Campo 'divida' não pode ser alterado manualmente. Faça o pagamento e evite multas maiores.");
        }

        // Atualiza apenas campos permitidos
        usuario.setNome(usuarioAtualizado.getNome());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setTelefone(usuarioAtualizado.getTelefone());
        // Adicione aqui outros campos que podem ser alterados

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

        // 🚫 Bloqueia exclusão se houver dívida
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

    // ✅ Quitar dívida de usuário
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
}
