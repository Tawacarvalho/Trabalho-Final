package com.locadora.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Item item;

    private Integer quantidade;

    private LocalDate dataEmprestimo;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataDevolucao;

    @Column(nullable = false)
    private Integer renovacoes = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private StatusEmprestimo status; // ACTIVE, RETURNED, LATE, BLOCKED

    @Column(nullable = false)
    private Double multa = 0.0;

    // Enum interno para status — mais seguro que String
    public enum StatusEmprestimo {
        ACTIVE,      // Em andamento
        RETURNED,    // Devolvido
        LATE,        // Atrasado
        BLOCKED      // Bloqueado por dívida
    }
}
