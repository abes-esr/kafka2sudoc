package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import fr.abes.kafkatosudoc.dto.mail.MailDto;
import fr.abes.kafkatosudoc.kafka.WorkInProgress;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EmailService {

    @Value("${mail.ws.recipient}")
    private String recipient;

    @Value("${mail.ws.url}")
    protected String url;

    @Value("${spring.profiles.active}")
    private String env;

    public void sendErrorsMessageCreateFromKafka(String filename, WorkInProgress<LigneKbartConnect> workInProgress) throws IOException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.CONNEXION)).count() + " erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets)", formatErrorMessageListToJson(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.CONNEXION)).toList()))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.DATE_FORMAT)).count() + " erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets)", formatErrorMessageListToJson(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.DATE_FORMAT)).toList()))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.ADD469)).count() + " erreur(s) d'ajout de 469", formatErrorMessageListToJson(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.ADD469)).toList()))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.SUPP469)).count() + " erreur(s) de suppression de 469", formatErrorMessageListToJson(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.SUPP469)).toList()))
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    private JsonObjectBuilder formatErrorMessageListToJson(List<ErrorMessage> list) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        AtomicInteger i = new AtomicInteger();
        list.forEach(errorMessage -> {
            int key = i.getAndIncrement();
            jsonObjectBuilder.add(String.valueOf(key), errorMessage.getMessage());
        });
        return jsonObjectBuilder;
    }

    public void sendErrorMessagesExNihilo(String filename, WorkInProgress<LigneKbartConnect> workInProgress) throws IOException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.EXNIHILO)).count() + " erreur(s) lors de la création de notice(s) ExNihilo", workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.EXNIHILO)).toList().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    public void sendErrorMessagesImprime(String filename, WorkInProgress<LigneKbartImprime> workInProgress) throws IOException {
        JsonObject listErrors = Json.createObjectBuilder()
                .add("kbart info : ", getKbartInfo(filename))
                .add(workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.FROMIMPRIME)).count() + " erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé",workInProgress.getErrorMessages().stream().filter(m -> m.getType().equals(ERROR_TYPE.FROMIMPRIME)).toList().toString())
                .build();
        sendErrorsMessage(filename, listErrors);
    }

    private void sendErrorsMessage(String filename, JsonObject listErrors) throws IOException {
        // Création du chemin d'accès pour le fichier .csv
        Path jsonFilePath = Path.of("errorsList_" + filename + ".txt");

        //  Création du fichier
        createAttachment(listErrors, jsonFilePath);

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreurs lors du traitement sur le fichier " + filename, "[CONVERGENCE]["+env.toUpperCase()+"] Erreurs lors du traitement sur le fichier " + filename);

        //  Récupération du fichier
        File file = jsonFilePath.toFile();

        //  Envoi du message par mail
        sendMailWithFile(requestJson, file);

        // Suppression du fichier temporaire
        Files.deleteIfExists(jsonFilePath);

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

    public void sendErrorMailProviderPackageDeleted(List<String> listError, String filename) throws IOException {
        // Création du chemin d'accès pour le fichier .csv
        Path jsonFilePath = Path.of("errorsList_" + filename + ".json");

        // Création de la liste
        JsonObject listErrors = Json.createObjectBuilder()
                .add("Erreur lors de la suppression du package", formatListToJson(listError))
                .build();

        //  Création du fichier
        createAttachment(listErrors, jsonFilePath);

        // Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression des liens vers les notices bouquet", "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression des liens vers les notices bouquet");

        //  Récupération du fichier
        File file = jsonFilePath.toFile();

        //  Envoi du message par mail
        sendMailWithFile(requestJson, file);

        // Suppression du fichier temporaire
        Files.deleteIfExists(jsonFilePath);

        log.info("L'email a été correctement envoyé.");
    }

    private JsonObjectBuilder formatListToJson(List<String> list) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        AtomicInteger i = new AtomicInteger();
        list.forEach(errorMessage -> {
            int key = i.getAndIncrement();
            jsonObjectBuilder.add(String.valueOf(key), errorMessage);
        });
        return jsonObjectBuilder;
    }

    private void sendMail(String requestJson) {
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

    protected void sendMailWithFile(String requestJson, File f) {
        //  Création du l'adresse du ws d'envoi de mails
        HttpPost uploadFile = new HttpPost(this.url + "htmlMailAttachment/");

        //  Création du builder
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("mail", requestJson, ContentType.APPLICATION_JSON);

        try {
            builder.addBinaryBody(
                    "attachment",
                    new FileInputStream(f),
                    ContentType.APPLICATION_OCTET_STREAM,
                    f.getName()
            );
        } catch (FileNotFoundException e) {
            log.warn("Le fichier n'a pas été trouvé. " + e.getMessage());
        }

        //  Envoi du mail
        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(uploadFile);
        } catch (IOException e) {
            log.warn("Erreur lors de l'envoi du mail. " + e.getMessage());
        }
    }

    private String mailToJSON(String to, String subject, String text) {
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

    private JsonObject getKbartInfo(String filename) {
        String provider = CheckFiles.getProviderFromFilename(filename);
        String packageName = CheckFiles.getPackageFromFilename(filename);

        return Json.createObjectBuilder()
                .add("Provider", provider)
                .add("Package", packageName)
                .add("Date", CheckFiles.extractDateString(filename))
                .build();
    }

    protected void createAttachment(JsonObject dataLines, Path path) {
        try {
            //  Création du fichier
            Writer writer = Files.newBufferedWriter(path);

            // Inscription des lignes dans le fichier
            writer.write(String.valueOf(dataLines));

            //  Ferme le Writer
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
