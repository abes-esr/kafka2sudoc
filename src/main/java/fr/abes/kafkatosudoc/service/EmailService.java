package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.dto.connect.LigneKbartConnect;

public interface EmailService {
    void sendErrorMail(String filename, LigneKbartConnect dataLines, Exception e);
}
