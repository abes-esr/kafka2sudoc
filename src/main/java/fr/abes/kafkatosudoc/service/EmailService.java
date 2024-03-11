package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.dto.mail.MailDto;
import fr.abes.kafkatosudoc.kafka.WorkInProgress;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class EmailService {

    @Value("${mail.ws.recipient}")
    private String recipient;

    @Value("${mail.ws.url}")
    protected String url;

    @Value("${spring.profiles.active}")
    private String env;

    public void sendErrorsMessageCreateFromKafka(String filename, WorkInProgress workInProgress) throws JsonProcessingException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.CONNEXION)).count() + " erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets)", workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.CONNEXION)).toList().toString())
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.DATE_FORMAT)).count() + " erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets)", workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.DATE_FORMAT)).toList().toString())
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.ADD469)).count() + " erreur(s) d'ajout de 469", workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.ADD469)).toList().toString())
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.SUPP469)).count() + " erreur(s) de suppression de 469", workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.SUPP469)).toList().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void sendErrorMessagesExNihilo(String filename, WorkInProgress workInProgress) throws JsonProcessingException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.EXNIHILO)).count() + " erreur(s) lors de la création de notice(s) ExNihilo", workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.EXNIHILO)).toList().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void sendErrorMessagesImprime(String filename, WorkInProgress workInProgress) throws JsonProcessingException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.FROMIMPRIME)).count() + " erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé",workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.FROMIMPRIME)).toList().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void sendErrorsMessage(String filename, JsonObject listErrors) {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreurs lors du traitement sur le fichier " + filename, String.valueOf(listErrors));
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailSuppressionPackage(String packageName, String provider, Exception e) {
        String body = "Une erreur s'est produite lors de la recherche de la notice bouquet : " + packageName +
                " / " +
                provider +
                "<br /><br />Avec le message d'erreur suivant : " +
                e.getMessage();

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression du package " + packageName + " / " + provider, body);
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailProviderPackageDeleted(List<String> listError) {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression des liens vers les notices bouquet", String.valueOf(listError));
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    protected void sendMail(String requestJson) {
        RestTemplate restTemplate = new RestTemplate(); //appel ws qui envoie le mail
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestJson, headers);

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            restTemplate.postForObject(url + "htmlMail/", entity, String.class); //appel du ws avec
        } catch (Exception e) {
            log.error("Erreur dans l'envoi du mail d'erreur Sudoc" + e);
        }
        //  Création du l'adresse du ws d'envoi de mails
        HttpPost mail = new HttpPost(this.url + "htmlMail/");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(mail);
        } catch (IOException e) {
            log.error("Erreur lors de l'envoi du mail. " + e);
        }
    }

    protected String mailToJSON(String to, String subject, String text) {
        String json = "";
        ObjectMapper mapper = new ObjectMapper();
        MailDto mail = new MailDto();
        mail.setApp("kafka2sudoc");
        mail.setTo(to.split(";"));
        mail.setCc(new String[]{});
        mail.setCci(new String[]{});
        mail.setSubject(subject);
        mail.setText(text);
        try {
            json = mapper.writeValueAsString(mail);
        } catch (JsonProcessingException e) {
            log.error("Erreur lors du la création du mail. " + e);
        }
        return json;
    }

    public JsonObject getKbartInfo(String filename) {
        String provider = CheckFiles.getProviderFromFilename(filename);
        String packageName = CheckFiles.getPackageFromFilename(filename);

        return Json.createObjectBuilder()
                .add("Provider", provider)
                .add("Package", packageName)
                .add("Date", CheckFiles.extractDateString(filename))
                .build();
    }
}
