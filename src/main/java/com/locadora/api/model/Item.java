package com.locadora.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;
    private String categoria;

    private Integer quantidade; // Quantidade total cadastrada

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer emprestados; // Quantos estão emprestados

    // Garante que valores nulos não causem erro
    @PrePersist
    @PreUpdate
    public void validarCampos() {
        if (quantidade == null) quantidade = 0;
        if (emprestados == null) emprestados = 0;
    }

    // Retorna a quantidade disponível
    public Integer getDisponivel() {
        int total = (quantidade == null ? 0 : quantidade);
        int emp = (emprestados == null ? 0 : emprestados);
        return Math.max(total - emp, 0);
    }

    // Indica se o item está disponível
    public boolean isDisponivel() {
        return getDisponivel() > 0;
    }
}
