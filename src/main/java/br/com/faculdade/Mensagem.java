package br.com.faculdade;

import java.time.LocalDateTime;

public class Mensagem { public Long id; public String remetente; public String conteudo; public LocalDateTime timestamp;

    // Construtor vazio exigido por frameworks de serialização JSON
    public Mensagem() {}

    public Mensagem(Long id, String remetente, String conteudo) {
        this.id = id;
        this.remetente = remetente;
        this.conteudo = conteudo;
        this.timestamp = LocalDateTime.now();
    }

}