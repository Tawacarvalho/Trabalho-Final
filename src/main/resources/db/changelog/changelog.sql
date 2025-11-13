-- Liquibase formatted SQL

-- changeset tawaf:001 create-table-usuario
CREATE TABLE IF NOT EXISTS USUARIO (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       nome VARCHAR(100),
    email VARCHAR(100),
    telefone VARCHAR(20),
    divida DECIMAL(10,2)
    );

-- changeset tawaf:002 create-table-item
CREATE TABLE IF NOT EXISTS ITEM (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    nome VARCHAR(100),
    descricao VARCHAR(255),
    categoria VARCHAR(100),
    quantidade INT
    );

-- changeset tawaf:003 create-table-emprestimo
CREATE TABLE IF NOT EXISTS EMPRESTIMO (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          usuario_id BIGINT,
                                          item_id BIGINT,
                                          quantidade INT,
                                          data_emprestimo DATE,
                                          data_prevista_devolucao DATE,
                                          data_devolucao DATE,
                                          renovacoes INT DEFAULT 0,
                                          status VARCHAR(20),
    multa DECIMAL(10,2),
    FOREIGN KEY (usuario_id) REFERENCES USUARIO(id),
    FOREIGN KEY (item_id) REFERENCES ITEM(id)
    );

-- changeset tawaf:004 insert-dados-iniciais
INSERT INTO USUARIO (nome, email, telefone, divida) VALUES
                                                        ('João da Silva', 'joao@email.com', '51999999999', 0),
                                                        ('Maria Oliveira', 'maria@email.com', '51988888888', 0),
                                                        ('Carlos Souza', 'carlos@email.com', '51977777777', 0);

INSERT INTO ITEM (nome, descricao, categoria, quantidade) VALUES
                                                              ('Cortador de Grama', 'Equipamento de jardim', 'Jardinagem', 5),
                                                              ('Motocultivador', 'Máquina agrícola leve', 'Agrícola', 3),
                                                              ('Furadeira', 'Furadeira elétrica', 'Ferramentas', 10);
