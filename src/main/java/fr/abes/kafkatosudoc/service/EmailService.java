package fr.abes.kafkatosudoc.service;


import fr.abes.LigneKbartConnect;

public interface EmailService {
    void sendErrorMail(String filename, LigneKbartConnect dataLines, Exception e);
}
