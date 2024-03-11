package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        JsonObject errorMessagesDelete469 = getListErrorMessagesDelete469(filename, workInProgress);
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getListErrorMessagesConnectionCbs().size() + " erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets)", workInProgress.getListErrorMessagesConnectionCbs().toString())
                .add(workInProgress.getListErrorMessagesDateFormat().size() + " erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets)", workInProgress.getListErrorMessagesDateFormat().toString())
                .add(workInProgress.getListErrorMessagesAdd469().size() + " erreur(s) d'ajout de 469", workInProgress.getListErrorMessagesAdd469().toString())
                .add(workInProgress.getListErrorMessagesDelete469().size() + " erreur(s) de suppression de 469", errorMessagesDelete469)
//                .add(workInProgress.getListErrorMessagesDelete469().size() + " erreur(s) de suppression de 469", workInProgress.getListErrorMessagesDelete469().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void getErrorMessagesExNihilo(String filename, WorkInProgress workInProgress) throws JsonProcessingException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getListErrorMessageExNihilo().size() + " erreur(s) lors de la création de notice(s) ExNihilo", workInProgress.getListErrorMessageExNihilo().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void getErrorMessagesImprime(String filename, WorkInProgress workInProgress) throws JsonProcessingException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getListErrorMessagesImprime().size() + " erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé", workInProgress.getListErrorMessagesImprime().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void sendErrorsMessage(String filename, JsonObject listErrors) throws JsonProcessingException {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreurs lors du traitement sur le fichier " + filename, String.valueOf(listErrors));
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailSuppressionPackage(String packageName, String provider, Exception e) {
        StringBuilder body =  new StringBuilder("Une erreur s'est produite lors de la recherche de la notice bouquet : ");
        body.append(packageName);
        body.append(" / ");
        body.append(provider);
        body.append("<br /><br />Avec le message d'erreur suivant : ");
        body.append(e.getMessage());

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression du package " + packageName + " / " + provider, body.toString());
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
        String date = "";

        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(filename);
        if(matcher.find()){
            date = matcher.group(1);
        }

        JsonObject kbartInfo = Json.createObjectBuilder()
                .add("Provider", provider)
                .add("Package", packageName)
                .add("Date", date)
                .build();

        return kbartInfo;
    }

    public JsonObject getListErrorMessagesDelete469(String filename, WorkInProgress workInProgress) {
        JsonObject allErrorMessagesDelete469 = null;
        for (JsonObject errorMsg : workInProgress.getListErrorMessagesDelete469()) {
            allErrorMessagesDelete469.put(String.valueOf(errorMsg.get("Ppn")), errorMsg);
        }

        return allErrorMessagesDelete469;
    }

}
