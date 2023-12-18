package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.CBSException;
import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.dto.mail.MailDto;
import fr.abes.kafkatosudoc.exception.IllegalDateException;
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

@Slf4j
@Service
public class EmailService {

    @Value("${mail.ws.recipient}")
    private String recipient;

    @Value("${mail.ws.url}")
    protected String url;

    @Value("${spring.profiles.active}")
    private String env;

    public void sendErrorMailConnect(String filename, LigneKbartConnect kbart, Exception e) {
        StringBuilder body =  new StringBuilder("Une erreur s'est produite lors de la modification de la notice ");
        body.append(kbart.getBESTPPN());
        buildBody(filename, kbart, e, body);

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la modification de la notice " + kbart.getBESTPPN(), body.toString());
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailDate(String filename, PackageKbartDto packageKbartDto, IllegalDateException e) {
        String body = "Une erreur s'est produite lors du traitement du package dans le Sudoc. Provider : " + packageKbartDto.getProvider() +
                " Package : " + packageKbartDto.getPackageName() +
                " Date : " + packageKbartDto.getDatePackage() +
                " Format de date incorrect. Message : " + e.getMessage();
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors du traitement sur le fichier " + filename, body);
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailAuthentification(String filename, PackageKbartDto packageKbartDto, CBSException e) {
        String body = "Une erreur s'est produite lors de l'authentification sur le CBS. Provider : " + packageKbartDto.getProvider() +
                " Package : " + packageKbartDto.getPackageName() +
                " Date : " + packageKbartDto.getDatePackage() +
                " Message : " + e.getMessage();
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors du traitement sur le fichier " + filename, body);
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailImprime(String filename, LigneKbartImprime kbart, Exception e) {
        StringBuilder body =  new StringBuilder("Une erreur s'est produite lors de la création de la notice électronique à partir de l'imprimé n°");
        body.append(kbart.getPpn());
        buildBody(filename, kbart, e, body);

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la création de la notice électronique à partir de l'imprimé " + kbart.getPpn(), body.toString());
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

    public void sendErrorMailSuppression469(String ppn, String ppnNoticeBouquet, Exception e) {
        StringBuilder body =  new StringBuilder("Une erreur s'est produite lors de la suppression du lien vers la notice bouquet " + ppnNoticeBouquet + " dans la notice " + ppn);
        body.append(" avec l'erreur suivante : <br />");
        body.append(e.getMessage());

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression du lien vers la notice bouquet", body.toString());
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    private static void buildBody(String filename, org.apache.avro.specific.SpecificRecord kbart, Exception e, StringBuilder body) {
        body.append(" lors du traitement du fichier ");
        body.append(filename);
        body.append("<br /><br />");
        body.append("pour la ligne Kbart : ");
        body.append("<br>");
        body.append(kbart);
        body.append("<br /><br />");
        body.append("Le message d'erreur est le suivant : ");
        body.append(e.getMessage());
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

}
