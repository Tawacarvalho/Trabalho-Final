package com.locadora.api.controller;

import com.locadora.api.model.Emprestimo;
import com.locadora.api.model.Item;
import com.locadora.api.repository.ItemRepository;
import com.locadora.api.repository.EmprestimoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/itens")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    // LISTAR TODOS
    @GetMapping
    public List<Item> listar() {
        List<Item> itens = itemRepository.findAll();
        for (Item item : itens) {
            if (item.getEmprestados() == null) {
                item.setEmprestados(0);
            }
        }
        return itens;
    }

    // BUSCAR POR ID
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable("id") Long id) {
        Optional<Item> itemOpt = itemRepository.findById(id);
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            if (item.getEmprestados() == null) {
                item.setEmprestados(0);
            }
            return ResponseEntity.ok(item);
        } else {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("erro", "Item não encontrado"));
        }
    }

    // CRIAR NOVO ITEM
    // CRIAR NOVO ITEM
    @PostMapping
    public ResponseEntity<?> criarItem(@RequestBody Item item) {

        // 🔴 VALIDAÇÃO DE CAMPOS OBRIGATÓRIOS
        List<String> erros = new ArrayList<>();

        if (item.getNome() == null || item.getNome().trim().isEmpty()) {
            erros.add("O campo 'nome' é obrigatório.");
        }
        if (item.getDescricao() == null || item.getDescricao().trim().isEmpty()) {
            erros.add("O campo 'descricao' é obrigatório.");
        }
        if (item.getCategoria() == null || item.getCategoria().trim().isEmpty()) {
            erros.add("O campo 'categoria' é obrigatório.");
        }
        if (item.getQuantidade() == null) {
            erros.add("O campo 'quantidade' é obrigatório.");
        }

        if (!erros.isEmpty()) {
            Map<String, Object> respostaErro = new HashMap<>();
            respostaErro.put("status", "erro");
            respostaErro.put("mensagem", "Campos obrigatórios não preenchidos.");
            respostaErro.put("detalhes", erros);
            return ResponseEntity.badRequest().body(respostaErro);
        }

        // Aqui você provavelmente deve salvar o item:
        itemRepository.save(item);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(item.getId())
                .toUri();

        return ResponseEntity.created(uri).body(item);
    }

    // ATUALIZAR ITEM
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable("id") Long id, @RequestBody Item novoItem) {
        Optional<Item> existente = itemRepository.findById(id);

        if (!existente.isPresent()) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("erro", "Item não encontrado"));
        }

        List<String> erros = new ArrayList<>();

        if (novoItem.getEmprestados() != null &&
                !novoItem.getEmprestados().equals(existente.get().getEmprestados())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("erro",
                            "O campo 'emprestados' não pode ser alterado manualmente."));
        }

        if (novoItem.getNome() == null || novoItem.getNome().trim().isEmpty()) {
            erros.add("O campo 'nome' é obrigatório.");
        }
        if (novoItem.getDescricao() == null || novoItem.getDescricao().trim().isEmpty()) {
            erros.add("O campo 'descricao' é obrigatório.");
        }
        if (novoItem.getCategoria() == null || novoItem.getCategoria().trim().isEmpty()) {
            erros.add("O campo 'categoria' é obrigatório.");
        }
        if (novoItem.getQuantidade() == null) {
            erros.add("O campo 'quantidade' é obrigatório.");
        }

        if (!erros.isEmpty()) {
            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("status", "erro");
            resposta.put("mensagem", "Campos obrigatórios não preenchidos.");
            resposta.put("detalhes", erros);
            return ResponseEntity.badRequest().body(resposta);
        }

        Item item = existente.get();
        item.setNome(novoItem.getNome());
        item.setDescricao(novoItem.getDescricao());
        item.setCategoria(novoItem.getCategoria());
        item.setQuantidade(novoItem.getQuantidade());

        itemRepository.save(item);
        return ResponseEntity.ok(item);
    }

    // DELETAR ITEM
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarItem(@PathVariable("id") Long id) {

        Optional<Item> itemOpt = itemRepository.findById(id);

        if (!itemOpt.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Item não encontrado para exclusão.");
        }

        Item item = itemOpt.get();

        boolean itemEmprestado = emprestimoRepository.existsByItemId(item.getId());
        if (itemEmprestado) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("O item '" + item.getNome() + "' não pode ser excluído pois está emprestado.");
        }

        itemRepository.delete(item);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Item '" + item.getNome() + "' excluído com sucesso.");
    }

    // DISPONIBILIDADE + PARA QUEM ESTÁ EMPRESTADO
    @GetMapping("/{id}/disponibilidade")
    public ResponseEntity<?> verificarDisponibilidade(@PathVariable("id") Long id) {
        Optional<Item> itemOpt = itemRepository.findById(id);
        if (!itemOpt.isPresent()) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("erro", "Item não encontrado"));
        }

        Item item = itemOpt.get();
        if (item.getEmprestados() == null) {
            item.setEmprestados(0);
        }

        List<Emprestimo> emprestimosAtivos = emprestimoRepository.findByItemId(id)
                .stream()
                .filter(e -> e.getStatus() == Emprestimo.StatusEmprestimo.ACTIVE)
                .collect(Collectors.toList());

        List<String> usuariosComItem = emprestimosAtivos.stream()
                .map(e -> e.getUsuario().getNome())
                .collect(Collectors.toList());

        int disponivel = item.getQuantidade() - item.getEmprestados();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("item", item.getNome());
        resposta.put("quantidade_total", item.getQuantidade());
        resposta.put("emprestado_para", usuariosComItem);
        resposta.put("empréstimos_ativos", emprestimosAtivos.size());
        resposta.put("disponivel", disponivel);
        resposta.put("mensagem", disponivel > 0 ?
                "Item disponível para empréstimo" :
                "Item indisponível no momento");

        return ResponseEntity.ok(resposta);
    }

    // LISTAR DISPONIBILIDADE DE TODOS OS ITENS
    @GetMapping("/disponibilidade")
    public ResponseEntity<?> listarDisponibilidadeGeral() {

        List<Item> itens = itemRepository.findAll();

        List<Emprestimo> emprestimosAtivos =
                emprestimoRepository.findByStatus(Emprestimo.StatusEmprestimo.ACTIVE);

        Map<Long, Integer> emprestadosPorItem = emprestimosAtivos.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getItem().getId(),
                        Collectors.summingInt(Emprestimo::getQuantidade)
                ));

        List<Map<String, Object>> resposta = new ArrayList<>();

        for (Item item : itens) {
            int emprestados = emprestadosPorItem.getOrDefault(item.getId(), 0);
            int disponivel = item.getQuantidade() - emprestados;

            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("id", item.getId());
            dados.put("item", item.getNome());
            dados.put("quantidade_total", item.getQuantidade());
            dados.put("emprestados", emprestados);
            dados.put("disponivel", disponivel);

            resposta.add(dados);
        }

        return ResponseEntity.ok(resposta);
    }
}
