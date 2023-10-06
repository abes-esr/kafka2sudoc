package fr.abes.kafkatosudoc.service;


import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;

public interface EmailService {
    void sendErrorMailConnect(String filename, LigneKbartConnect dataLines, Exception e);

    void sendErrorMailImprime(String filename, LigneKbartImprime dataLines, Exception e);


}
