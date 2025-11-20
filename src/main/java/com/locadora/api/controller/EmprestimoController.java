package com.locadora.api.controller;

import com.locadora.api.model.Emprestimo;
import com.locadora.api.dto.EmprestimoRequest;
import com.locadora.api.service.EmprestimoService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/emprestimos")
@CrossOrigin(origins = "http://localhost:3000")
public class EmprestimoController {

    private final EmprestimoService service;

    public EmprestimoController(EmprestimoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Emprestimo> listar() {
        return service.listar();
    }

    @PostMapping
    public Emprestimo criar(@RequestBody EmprestimoRequest request) {
        return service.emprestar(
                request.getUsuarioId(),
                request.getItemId(),
                request.getQuantidade(),
                LocalDate.parse(request.getDataPrevistaDevolucao())
        );
    }

    @PostMapping("/{id}/devolver")
    public Emprestimo devolver(@PathVariable("id") Long id) {
        return service.devolver(id);
    }

    @PostMapping("/{id}/renovar")
    public Map<String, Object> renovar(@PathVariable("id") Long id,
                                       @RequestParam(value = "diasExtra", required = false) Integer diasExtra) {

        Emprestimo emprestimo = service.renovar(id, diasExtra);

        Map<String, Object> response = new HashMap<>();
        response.put("id", emprestimo.getId());
        response.put("status", emprestimo.getStatus().name());
        response.put("renovacoes", emprestimo.getRenovacoes());
        response.put("novaDataPrevistaDevolucao", emprestimo.getDataPrevistaDevolucao());
        response.put("mensagem", "Renovação realizada com sucesso.");

        return response;
    }


    @GetMapping("/dividas/{usuarioId}")
    public List<Emprestimo> consultarDividas(@PathVariable("usuarioId") Long usuarioId) {
        return service.consultarDividasUsuario(usuarioId);
    }

    @GetMapping("/{id}")
    public Object consultarEmprestimo(@PathVariable("id") Long id) {
        Emprestimo emprestimo = service.consultarEmprestimo(id);

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("id", emprestimo.getId());
        response.put("status", emprestimo.getStatus().getLabel());
        response.put("multa", emprestimo.getMulta());
        response.put("dataEmprestimo", emprestimo.getDataEmprestimo());
        response.put("dataPrevistaDevolucao", emprestimo.getDataPrevistaDevolucao());
        response.put("dataDevolucao", emprestimo.getDataDevolucao());
        response.put("usuario", emprestimo.getUsuario().getNome());
        response.put("item", emprestimo.getItem().getNome());

        return response;
    }
}
