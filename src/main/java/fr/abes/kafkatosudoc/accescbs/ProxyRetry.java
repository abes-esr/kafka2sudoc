package fr.abes.kafkatosudoc.accescbs;

import fr.abes.cbs.exception.CBSException;
import fr.abes.kafkatosudoc.service.TraitementService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProxyRetry {
    @Autowired
    TraitementService service;

    /**
     * permet de retenter plusieurs fois la connexion à CBS
     * @param login identifiant de connexion
     * @throws CBSException rejet de la connexion
     */
    @Retryable
    public void authenticate(String login) throws CBSException {
        log.warn("Authentification avec le login" + login);
        service.authenticate(login);
    }

    /**
     * Méthode de modification d'un exemplaire existant dans le CBS (4 tentatives max)
     * @param noticeTraitee notice modifiée
     * @param epn epn de la notice à modifier
     * @throws CBSException : erreur CBS
     */
    @Retryable(maxAttempts = 4, include = Exception.class,
            exclude = CBSException.class, backoff = @Backoff(delay = 1000, multiplier = 2) )
    public void saveExemplaire(String noticeTraitee, String epn) throws CBSException {
        service.saveExemplaire(noticeTraitee, epn);
    }

}
