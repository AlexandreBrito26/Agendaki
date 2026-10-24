package com.agendaki.whatsapp;

/** A Cloud API recusou ou não respondeu ao envio. A mensagem nunca contém o token de acesso. */
public class WhatsAppSendException extends RuntimeException {

    public WhatsAppSendException(String message) {
        super(message);
    }

    public WhatsAppSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
