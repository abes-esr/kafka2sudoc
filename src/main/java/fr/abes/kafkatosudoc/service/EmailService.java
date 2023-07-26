package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.dto.LigneKbartDto;

public interface EmailService {
    void sendErrorMail(String filename, LigneKbartDto dataLines, Exception e);
}
