package fr.abes.kafkatosudoc.kafka;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.cbs.notices.Zone;
import fr.abes.kafkatosudoc.dto.KbartAndImprimeDto;
import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.entity.LigneKbart;
import fr.abes.kafkatosudoc.entity.ProviderPackage;
import fr.abes.kafkatosudoc.exception.IllegalDateException;
import fr.abes.kafkatosudoc.service.BaconService;
import fr.abes.kafkatosudoc.service.EmailService;
import fr.abes.kafkatosudoc.service.SudocService;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import fr.abes.kafkatosudoc.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class KbartListener {

    @Value("${sudoc.serveur}")
    private String serveurSudoc;

    @Value("${sudoc.port}")
    private String portSudoc;

    @Value("${sudoc.password}")
    private String passwordSudoc;

    @Value("${sudoc.login}")
    private String loginSudoc;

    @Value("${sudoc.signalDb}")
    private String signalDb;

    private final UtilsMapper mapper;


    private final BaconService baconService;

    private final EmailService emailService;

    private final Map<String, WorkInProgress> workInProgressMap;

    private final Map<String, WorkInProgress> workInProgressMapExNihilo;

    private final Map<String, WorkInProgress> workInProgressMapImprime;

    public KbartListener(UtilsMapper mapper, BaconService baconService, EmailService emailService, Map<String, WorkInProgress> workInProgressMap, Map<String, WorkInProgress> workInProgressMapExNihilo, Map<String, WorkInProgress> workInProgressMapImprime) {
        this.mapper = mapper;
        this.baconService = baconService;
        this.emailService = emailService;
        this.workInProgressMap = workInProgressMap;
        this.workInProgressMapExNihilo = workInProgressMapExNihilo;
        this.workInProgressMapImprime = workInProgressMapImprime;
    }

    /**
     * Listener pour la modification de notices biblio bestPpn (ajout 469)
     *
     * @param lignesKbart : ligne trouvée dans kafka
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.toload}"}, groupId = "${topic.groupid.source.withppn}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToCreateFromKafka(ConsumerRecord<String, LigneKbartConnect> lignesKbart) {
        String filename = lignesKbart.key();
        if (!this.workInProgressMap.containsKey(filename))
            this.workInProgressMap.put(lignesKbart.key(), new WorkInProgress());

        if (lignesKbart.value().getBESTPPN() != null && !lignesKbart.value().getBESTPPN().isEmpty()) {
            //on alimente la liste des notices d'un package qui sera traitée intégralement
            this.workInProgressMap.get(filename).addNotice(lignesKbart.value());
        }
        this.workInProgressMap.get(filename).incrementCurrentNbLignes();
        for (Header header : lignesKbart.headers().toArray()) {
            if (header.key().equals("nbLinesTotal")) { //Si on est à la dernière ligne du fichier
                this.workInProgressMap.get(filename).setNbLinesTotal(Integer.parseInt(new String(header.value()))); //on indique le nb total de lignes du fichier
            }
        }
        //Si le nombre de lignes traitées est égal au nombre de lignes total du fichier, on est arrivé en fin de fichier, on traite dans le sudoc
        if (this.workInProgressMap.get(filename).getCurrentNbLines().equals(this.workInProgressMap.get(filename).getNbLinesTotal())) {
            log.debug("Traitement des notices existantes dans le Sudoc");
            traiterPackageDansSudoc(this.workInProgressMap.get(filename).getListeNotices(), filename);
            if (!this.workInProgressMap.get(filename).isErrorFree()) emailService.sendErrorsMessage(this.workInProgressMap.get(filename).getAllErrorMessages(filename), filename);
            this.workInProgressMap.remove(filename);
        }

    }

    private void traiterPackageDansSudoc(List<LigneKbartConnect> listeNotices, String filename) {
        PackageKbartDto packageKbartDto = new PackageKbartDto();
        List<String> newBestPpn = new ArrayList<>();
        List<String> deletedBestPpn = new ArrayList<>();
        SudocService service = new SudocService();
        try {
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            Date dateFromFile = CheckFiles.extractDate(filename);
            packageKbartDto = new PackageKbartDto(packageName, dateFromFile, provider);

            ProviderPackage lastPackage = baconService.findLastVersionOfPackage(packageKbartDto);
            service.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
            String ppnNoticeBouquet = service.getNoticeBouquet(packageKbartDto.getProvider(), packageKbartDto.getPackageName());
            //cas ou on a une version antérieure de package
            Set<LigneKbart> ppnLastVersion = new HashSet<>();
            if (lastPackage != null) {
                ppnLastVersion = baconService.findAllPpnFromPackage(lastPackage);
                for (String ppn : ppnLastVersion.stream().map(LigneKbart::getBestPpn).toList()) {
                    if (!(listeNotices.stream().map(ligneKbartConnect -> ligneKbartConnect.getBESTPPN().toString()).toList().contains(ppn)))
                        deletedBestPpn.add(ppn);
                }
                for (CharSequence ppn : listeNotices.stream().map(LigneKbartConnect::getBESTPPN).toList()) {
                    if (!ppnLastVersion.stream().map(LigneKbart::getBestPpn).toList().contains(ppn.toString()))
                        newBestPpn.add(ppn.toString());
                }
            } else {
                //pas de version antérieure, tous les bestPpn sont nouveaux
                newBestPpn.addAll(listeNotices.stream().map(ligneKbartConnect -> ligneKbartConnect.getBESTPPN().toString()).toList());
            }
            //traitement des notices dans le cbs : ajout ou suppression de 469 en fonction des cas
            for (String ppn : newBestPpn) {
                ajout469(ppnNoticeBouquet, ppn, listeNotices.stream().filter(ligneKbartConnect -> ligneKbartConnect.getBESTPPN().toString().equals(ppn)).findFirst().get(), filename, service);
            }

            for (String ppn : deletedBestPpn) {
                suppression469(ppnNoticeBouquet, ppn, mapper.map(ppnLastVersion.stream().filter(ligne -> ligne.getBestPpn().equals(ppn)).findFirst().get(), LigneKbartConnect.class), filename, service);
            }
        } catch (CBSException e) {
            log.error(e.getMessage(), e.getCause());
            this.workInProgressMap.get(filename).addErrorMessagesConnectionCbs(
                    "Provider : " + packageKbartDto.getProvider() +
                    " - Package : " + packageKbartDto.getPackageName() +
                    " - Date : " + packageKbartDto.getDatePackage() +
                    " - Erreur : " + e.getMessage());
        } catch (IllegalDateException e) {
            log.error("Erreur lors du traitement du package dans le Sudoc : format de date incorrect", e.getCause());
            this.workInProgressMap.get(filename).addErrorMessagesDateFormat(
                    "Provider : " + packageKbartDto.getProvider() +
                    " - Package : " + packageKbartDto.getPackageName() +
                    " - Date : " + packageKbartDto.getDatePackage() +
                    " - Erreur : " + e.getMessage());
        } finally {
            try {
                service.disconnect();
            } catch (CBSException e) {
                log.warn("Erreur de déconnexion du Sudoc");
            }
        }
    }

    private void ajout469(String ppnNoticeBouquet, String ppn, LigneKbartConnect ligneKbart, String filename, SudocService service) {
        try {
            NoticeConcrete notice = service.getNoticeFromPpn(ppn);
            if (!service.isNoticeBouquetInPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                service.addNoticeBouquetInPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                service.modifierNotice(notice, 1);
                log.debug("Ajout 469 : Notice " + notice.getNoticeBiblio().findZone("003", 0).getValeur() + " modifiée avec succès");
            }
        } catch (CBSException | ZoneException e) {
            String message = "PPN : " + ppn + " : " + e.getMessage();
            log.error(message, e.getCause());
            this.workInProgressMap.get(filename).addErrorMessagesAdd469(
                    "PPN : " + ppn + " : " +
                    " - Ligne Kbart : " + ligneKbart +
                    " - Erreur : " + e.getMessage());
        }
    }

    private void suppression469(String ppnNoticeBouquet, String ppn, LigneKbartConnect ligneKbart, String filename, SudocService service) {
        try {
            NoticeConcrete notice = service.getNoticeFromPpn(ppn);
            if (service.isNoticeBouquetInPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                service.supprimeNoticeBouquetInPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                service.modifierNotice(notice, 1);
                log.debug("Suppression 469 : Notice " + notice.getNoticeBiblio().findZone("003", 0).getValeur() + " modifiée avec succès");
            }
        } catch (CBSException | ZoneException e) {
            String message = "PPN : " + ppn + " : " + e.getMessage();
            log.error(message, e.getCause());
            this.workInProgressMap.get(filename).addErrorMessagesDelete469(
                    "PPN : " + ppn + " : " +
                    " - Ligne Kbart : " + ligneKbart +
                    " - -Erreur : " + e.getMessage());
        }
    }

    /**
     * Listener pour modification notice biblio (suppression 469)
     *
     * @param providerPackageDeleted enregistrement dans kafka
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.todelete}"}, groupId = "${topic.groupid.source.delete}", containerFactory = "kafkaDeletePackageListenerContainerFactory")
    public void listenKbartToDeleteFromKafka(ConsumerRecord<String, GenericRecord> providerPackageDeleted) {
        String provider = providerPackageDeleted.value().get("PROVIDER").toString();
        String packageName = providerPackageDeleted.value().get("PACKAGE").toString();
        SudocService service = new SudocService();
        try {
            service.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
            //recherche de la notice bouquet
            String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
            //affichage des notices liées
            //boucle sur les notices liées à partir de la seconde (la première étant la notice bouquet elle-même)
            int nbNoticesLiees = service.getNoticesLiees();
            List<String> listError = new ArrayList<>();
            for (int i = 2; i <= nbNoticesLiees; i++) {
                String ppnCourant = "";
                try {
                    service.voirNotice(i);
                    NoticeConcrete notice = service.passageEditionNotice(i);
                    ppnCourant = notice.getNoticeBiblio().findZone("003", 0).getValeur();
                    log.debug(ppnCourant);
                    if (service.isNoticeBouquetInPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                        service.supprimeNoticeBouquetInPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                        service.modifierNotice(notice, i);
                        log.debug("Suppression 469 : Notice " + notice.getNoticeBiblio().findZones("003").get(0).getValeur() + " modifiée avec succès");
                    }
                } catch (CBSException | ZoneException e) {
                    log.error(e.getMessage(), e.getCause());
                    listError.add("Notice bouquet " + ppnNoticeBouquet + " - notice " + ppnCourant + " erreur : " + e.getMessage());
                    service.retourArriere();
                }
            }
            if (!listError.isEmpty()) {
                listError.add(0, listError.size() +  " erreur(s) lors de la suppression de lien(s) vers une notice bouquet : " + System.lineSeparator());
                emailService.sendErrorMailProviderPackageDeleted(listError);
            }
        } catch (CBSException e) {
            log.error(e.getMessage(), e.getCause());
            emailService.sendErrorMailSuppressionPackage(packageName, provider, e);
        } finally {
            try {
                service.disconnect();
            } catch (CBSException e) {
                log.warn("Erreur de déconnexion du Sudoc");
            }
        }

    }

    /**
     * @param ligneKbart : enregistrement dans Kafka
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.exnihilo}"}, groupId = "${topic.groupid.source.exnihilo}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafkaExNihilo(ConsumerRecord<String, LigneKbartConnect> ligneKbart) {
        log.debug("Entrée dans création ex nihilo");
        String filename = ligneKbart.key();

        // S'il s'agit d'un premier message d'un fichier kbart, on créé un WorkInProgress avec le nom du fichier et le nombre total de ligne
        if (!this.workInProgressMapExNihilo.containsKey(filename)) {
            this.workInProgressMapExNihilo.put(filename, new WorkInProgress());
            for (Header header : ligneKbart.headers().toArray()) {
                if (header.key().equals("nbLinesTotal")) { //Si on est à la dernière ligne du fichier
                    this.workInProgressMapExNihilo.get(filename).setNbLinesTotal(Integer.parseInt(new String(header.value()))); //on indique le nb total de lignes du fichier
                }
            }
        }

        // On incrémente le compteur de ligne et on ajoute chaque ligne dans le WorkInProgress associé au nom du fichier kbart
        this.workInProgressMapExNihilo.get(filename).incrementCurrentNbLignes();
        this.workInProgressMapExNihilo.get(filename).addNotice(ligneKbart.value());

        //Si le nombre de lignes traitées est égal au nombre de lignes total du fichier, on est arrivé en fin de fichier, on traite dans le sudoc
        if (this.workInProgressMapExNihilo.get(filename).getCurrentNbLines().equals(this.workInProgressMapExNihilo.get(filename).getNbLinesTotal())) {
            log.debug("Traitement des notices existantes dans le Sudoc");
            SudocService service = new SudocService();
            try {
                String provider = CheckFiles.getProviderFromFilename(filename);
                String packageName = CheckFiles.getPackageFromFilename(filename);
                service.authenticateBaseSignal(serveurSudoc, portSudoc, loginSudoc, passwordSudoc, signalDb);

                if (this.workInProgressMapExNihilo.get(filename).getListeNotices() != null && !this.workInProgressMapExNihilo.get(filename).getListeNotices().isEmpty()) {
                    for (LigneKbartConnect ligneKbartConnect : this.workInProgressMapExNihilo.get(filename).getListeNotices()) {
                        NoticeConcrete notice = mapper.map(ligneKbartConnect, NoticeConcrete.class);
                        //Ajout provider display name en 214 $c 2è occurrence
                        String providerDisplay = baconService.getProviderDisplayName(provider);
                        if (providerDisplay != null) {
                            notice.getNoticeBiblio().findZone("214", 1).addSubLabel("$c", providerDisplay);
                        }
                        service.addLibelleNoticeBouquetInPpn(notice.getNoticeBiblio(), provider + "_" + packageName);
                        service.creerNotice(notice);
                        log.debug("Ajout notice exNihilo effectué");
                    }
                }

            } catch (CBSException | ZoneException e) {
                log.error(e.getMessage());
                this.workInProgressMapExNihilo.get(filename).addErrorMessageExNihilo("BestPpn : " + ligneKbart.value().getBESTPPN() + " - erreur : " + e.getMessage());
            } finally {
                try {
                    // On déconnecte du Sudoc, on envoie les messages d'erreurs s'il y a des erreurs et on supprime le WorkInProgress
                    service.disconnect();
                    if (!this.workInProgressMapExNihilo.get(filename).isErrorFree()) emailService.sendErrorsMessage(this.workInProgressMapExNihilo.get(filename).getAllErrorMessages(filename), filename);
                    this.workInProgressMapExNihilo.remove(filename);
                } catch (CBSException e) {
                    log.warn("Erreur de déconnexion du Sudoc");
                }
            }
        }

    }

    /**
     * Listener Kafka pour la création de notices électronique à partir du kbart et de la notice imprimée
     *
     * @param lignesKbart : ligne kbart + ppn de la notice imprimée
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.imprime}"}, groupId = "${topic.groupid.source.imprime}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafkaImprime(ConsumerRecord<String, LigneKbartImprime> lignesKbart) {
        log.debug("entree dans création from imprimé et kbart");
        String filename = lignesKbart.key();

        // S'il s'agit d'un premier message d'un fichier kbart, on créé un WorkInProgress avec le nom du fichier et le nombre total de ligne
        if (!this.workInProgressMapImprime.containsKey(filename)) {
            this.workInProgressMapImprime.put(filename, new WorkInProgress());
            for (Header header : lignesKbart.headers().toArray()) {
                if (header.key().equals("nbLinesTotal")) { //Si on est à la dernière ligne du fichier
                    this.workInProgressMapImprime.get(filename).setNbLinesTotal(Integer.parseInt(new String(header.value()))); //on indique le nb total de lignes du fichier
                }
            }
        }

        // On incrémente le compteur de ligne et on ajoute chaque ligne dans le WorkInProgress associé au nom du fichier kbart
        this.workInProgressMapImprime.get(filename).incrementCurrentNbLignes();
        this.workInProgressMapImprime.get(filename).addNoticeImprime(lignesKbart.value());

        //Si le nombre de lignes traitées est égal au nombre de lignes total du fichier, on est arrivé en fin de fichier, on traite dans le sudoc
        if (this.workInProgressMapImprime.get(filename).getCurrentNbLines().equals(this.workInProgressMapImprime.get(filename).getNbLinesTotal())) {
            log.debug("Traitement des notices existantes dans le Sudoc");

            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            SudocService service = new SudocService();
            try {
                //authentification sur la base maitre du sudoc pour récupérer la notice imprimée
                service.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);

                if (this.workInProgressMapImprime.get(filename).getListeNoticesImprime() != null && !this.workInProgressMapImprime.get(filename).getListeNoticesImprime().isEmpty()) {
                    for (LigneKbartImprime ligneKbartImprime : this.workInProgressMapImprime.get(filename).getListeNoticesImprime()) {
                        KbartAndImprimeDto kbartAndImprimeDto = new KbartAndImprimeDto();
                        kbartAndImprimeDto.setKbart(mapper.map(ligneKbartImprime, LigneKbartImprime.class));
                        kbartAndImprimeDto.setNotice(service.getNoticeFromPpn(ligneKbartImprime.getPpn().toString()));
                        NoticeConcrete noticeElec = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
                        //Ajout provider display name en 214 $c 2è occurrence
                        String providerDisplay = baconService.getProviderDisplayName(provider);
                        if (providerDisplay != null) {
                            List<Zone> zones214 = noticeElec.getNoticeBiblio().findZones("214").stream().filter(zone -> Arrays.toString(zone.getIndicateurs()).equals("[#, 2]")).toList();
                            for (Zone zone : zones214)
                                zone.addSubLabel("c", providerDisplay);
                        }
                        service.addLibelleNoticeBouquetInPpn(noticeElec.getNoticeBiblio(), provider + "_" + packageName);
                        service.creerNotice(noticeElec);
                        log.debug("Création notice à partir de l'imprimée terminée");
                    }
                }
            } catch (CBSException | ZoneException e) {
                log.error(e.getMessage());
                this.workInProgressMapImprime.get(filename).addErrorMessagesImprime("BestPpn : " + lignesKbart.value().getPpn() + " - erreur : " + e.getMessage());
            } finally {
                try {
                    // On déconnecte du Sudoc, on envoie les messages d'erreurs s'il y a des erreurs et on supprime le WorkInProgress
                    service.disconnect();
                    if (!this.workInProgressMapImprime.get(filename).isErrorFree()) emailService.sendErrorsMessage(this.workInProgressMapImprime.get(filename).getAllErrorMessages(filename), filename);
                    this.workInProgressMapImprime.remove(filename);
                } catch (CBSException e) {
                    log.warn("Erreur de déconnexion du Sudoc");
                }
            }
        }

    }
}
