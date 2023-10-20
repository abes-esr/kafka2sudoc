package fr.abes.kafkatosudoc.kafka;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.NoticeConcrete;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class KbartListener {
    private final UtilsMapper mapper;

    private final SudocService service;

    private final BaconService baconService;

    private final EmailService emailService;

    private final List<LigneKbartConnect> listeNotices = new ArrayList<>();

    private String filename = "";
    public KbartListener(UtilsMapper mapper, SudocService service, BaconService baconService, EmailService emailService) {
        this.mapper = mapper;
        this.service = service;
        this.baconService = baconService;
        this.emailService = emailService;
    }

    /**
     * Listener pour la modification de notices biblio bestPpn (ajout 469)
     *
     * @param lignesKbart : ligne trouvée dans kafka
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.toload}"}, groupId = "${topic.groupid.source.withppn}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToCreateFromKafka(ConsumerRecord<String, LigneKbartConnect> lignesKbart) throws IllegalDateException {
        this.filename = getFileNameFromHeader(lignesKbart.headers());
        String provider = CheckFiles.getProviderFromFilename(this.filename);
        String packageName = CheckFiles.getPackageFromFilename(this.filename);
        Date dateFromFile = CheckFiles.extractDate(this.filename);
        PackageKbartDto packageKbartDto = new PackageKbartDto(packageName, dateFromFile, provider);

        if (lignesKbart.value().getBESTPPN() != null && !lignesKbart.value().getBESTPPN().isEmpty()) {
            //on alimente la liste des notices d'un package qui sera traitée intégralement
            this.listeNotices.add(lignesKbart.value());
        }
        for (Header header : lignesKbart.headers().toArray()) {
            if (header.key().equals("OK") && new String(header.value()).equals("true")) {
                traiterPackageDansSudoc(listeNotices, packageKbartDto);
                this.listeNotices.clear();
                this.filename = "";
                break;
            }
        }
    }

    private void traiterPackageDansSudoc(List<LigneKbartConnect> listeNotices, PackageKbartDto packageKbartDto) {
        List<String> newBestPpn = new ArrayList<>();
        List<String> deletedBestPpn = new ArrayList<>();

        ProviderPackage lastPackage = baconService.findLastVersionOfPackage(packageKbartDto);
        try {
            service.authenticate();
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
                ajout469(ppnNoticeBouquet, ppn, listeNotices.stream().filter(ligneKbartConnect -> ligneKbartConnect.getBESTPPN().toString().equals(ppn)).findFirst().get());
            }

            for (String ppn : deletedBestPpn) {
                suppression469(ppnNoticeBouquet, ppn, constructLigneKbartConnect(ppnLastVersion.stream().filter(ligne -> ligne.getBestPpn().equals(ppn)).findFirst().get()));
            }
        } catch (CBSException e) {
            log.error(e.getMessage(), e.getCause());
            emailService.sendErrorMailAuthentification(this.filename, packageKbartDto, e);
        } finally {
            try {
                service.disconnect();
            } catch (CBSException e) {
                log.error("Impossible de se déconnecter du cbs");
            }
        }
    }

    private LigneKbartConnect constructLigneKbartConnect(LigneKbart ligneKbart) {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        return LigneKbartConnect.newBuilder()
                .setPUBLICATIONTITLE(ligneKbart.getPublicationTitle())
                .setPUBLICATIONTYPE(ligneKbart.getPublicationType())
                .setPRINTIDENTIFIER(ligneKbart.getPrintIdentifier())
                .setONLINEIDENTIFIER(ligneKbart.getOnlineIdentifer())
                .setDATEFIRSTISSUEONLINE(ligneKbart.getDateFirstIssueOnline() != null ? format.format(ligneKbart.getDateFirstIssueOnline()) : null)
                .setNUMFIRSTVOLONLINE(ligneKbart.getNumFirstVolOnline())
                .setNUMFIRSTISSUEONLINE(ligneKbart.getNumFirstIssueOnline())
                .setDATELASTISSUEONLINE(ligneKbart.getDateLastIssueOnline() != null ? format.format(ligneKbart.getDateLastIssueOnline()) : null)
                .setNUMLASTVOLONLINE(ligneKbart.getNumLastVolOnline())
                .setNUMLASTISSUEONLINE(ligneKbart.getNumlastIssueOnline())
                .setTITLEURL(ligneKbart.getTitleUrl())
                .setFIRSTAUTHOR(ligneKbart.getFirstAuthor())
                .setTITLEID(ligneKbart.getTitleId())
                .setEMBARGOINFO(ligneKbart.getEmbargoInfo())
                .setCOVERAGEDEPTH(ligneKbart.getCoverageDepth())
                .setNOTES(ligneKbart.getNotes())
                .setPUBLISHERNAME(ligneKbart.getPublisherName())
                .setPUBLICATIONTYPE(ligneKbart.getPublicationType())
                .setDATEMONOGRAPHPUBLISHEDPRINT(ligneKbart.getDateMonographPublishedPrint() != null ? format.format(ligneKbart.getDateMonographPublishedPrint()) : null)
                .setDATEMONOGRAPHPUBLISHEDONLIN(ligneKbart.getDateMonographPublishedOnline() != null ? format.format(ligneKbart.getDateMonographPublishedOnline()) : null)
                .setMONOGRAPHVOLUME(ligneKbart.getMonographVolume())
                .setMONOGRAPHEDITION(ligneKbart.getMonographEdition())
                .setFIRSTEDITOR(ligneKbart.getFirstEditor())
                .setPARENTPUBLICATIONTITLEID(ligneKbart.getParentPublicationTitleId())
                .setPRECEDINGPUBLICATIONTITLEID(ligneKbart.getPrecedeingPublicationTitleId())
                .setACCESSTYPE(ligneKbart.getAccessType())
                .setPROVIDERPACKAGEPACKAGE(ligneKbart.getProviderPackage().getProviderPackageId().getPackageName())
                .setPROVIDERPACKAGEDATEP(ligneKbart.getProviderPackage().getProviderPackageId().getDateP().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .setPROVIDERPACKAGEIDTPROVIDER(ligneKbart.getProviderPackage().getProvider().getIdtProvider())
                .setBESTPPN(ligneKbart.getBestPpn())
                .build();

    }

    private void ajout469(String ppnNoticeBouquet, String ppn, LigneKbartConnect ligneKbart) {
        try {
            NoticeConcrete notice = service.getNoticeFromPpn(ppn);
            if (!service.isNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                service.addNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                service.modifierNotice(notice);
                log.debug("Ajout 469 : Notice " + notice.getNoticeBiblio().findZone("003", 0).getValeur() + " modifiée avec succès");
            }
        } catch (CBSException | ZoneException e) {
            String message = "PPN : " + ppn + " : " + e.getMessage();
            log.error(message, e.getCause());
            emailService.sendErrorMailConnect(this.filename, ligneKbart, e);
        }
    }

    private void suppression469(String ppnNoticeBouquet, String ppn, LigneKbartConnect ligneKbart) {
        try {
            NoticeConcrete notice = service.getNoticeFromPpn(ppn);
            if (service.isNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                service.supprimeNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                service.modifierNotice(notice);
                log.debug("Suppression 469 : Notice " + notice.getNoticeBiblio().findZone("003", 0).getValeur() + " modifiée avec succès");
            }
        } catch (CBSException | ZoneException e) {
            String message = "PPN : " + ppn + " : " + e.getMessage();
            log.error(message, e.getCause());
            emailService.sendErrorMailConnect(this.filename, ligneKbart, e);
        }
    }

    /**
     * Listener pour modification notice biblio (suppression 469)
     *
     * @param lignesKbart enregistrement dans kafka
     * @throws CBSException : erreur CBS
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.todelete}"}, groupId = "${topic.groupid.source.delete}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToDeleteFromKafka(ConsumerRecord<String, LigneKbartConnect> lignesKbart) throws CBSException {
        String filename = "";
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            if (!lignesKbart.value().getBESTPPN().isEmpty()) {
                service.authenticate();
                String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
                NoticeConcrete noticeBestPpn = service.getNoticeFromPpn(lignesKbart.value().getBESTPPN().toString());
                if (service.isNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet)) {
                    service.supprimeNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet);
                    service.modifierNotice(noticeBestPpn);
                    log.debug("Suppression 469 : Notice " + lignesKbart.value().getBESTPPN().toString() + " modifiée avec succès");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            emailService.sendErrorMailConnect(filename, lignesKbart.value(), e);
        } finally {
            service.disconnect();
        }
    }

    /**
     * @param lignesKbart : enregistrement dans Kafka
     * @throws CBSException : erreur CBS
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.exnihilo}"}, groupId = "${topic.groupid.source.exnihilo}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafkaExNihilo(ConsumerRecord<String, LigneKbartConnect> lignesKbart) throws CBSException {
        String filename = "";
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());
            String provider = CheckFiles.getProviderFromFilename(filename);
            service.authenticate();
            NoticeConcrete notice = mapper.map(lignesKbart.value(), NoticeConcrete.class);
            //Ajout provider display name en 214 $c 2è occurrence
            String providerDisplay = baconService.getProviderDisplayName(provider);
            if (providerDisplay != null) {
                notice.getNoticeBiblio().findZone("214", 1).addSubLabel("$c", providerDisplay);
            }
            service.creerNotice(notice);
            log.debug("Ajout notice exNihilo effectué");
        } catch (CBSException | ZoneException e) {
            log.error(e.getMessage());
            emailService.sendErrorMailConnect(filename, lignesKbart.value(), e);
        } finally {
            service.disconnect();
        }
    }

    /**
     * Listener Kafka pour la création de notices électronique à partir du kbart et de la notice imprimée
     *
     * @param lignesKbart : ligne kbart + ppn de la notice imprimée
     * @throws CBSException : erreur liée au cbs
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.imprime}"}, groupId = "${topic.groupid.source.imprime}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafkaImprime(ConsumerRecord<String, LigneKbartImprime> lignesKbart) throws CBSException {
        String filename = getFileNameFromHeader(lignesKbart.headers());
        String provider = CheckFiles.getProviderFromFilename(filename);
        String packageName = CheckFiles.getPackageFromFilename(filename);
        try {
            service.authenticate();
            KbartAndImprimeDto kbartAndImprimeDto = new KbartAndImprimeDto();
            kbartAndImprimeDto.setKbart(mapper.map(lignesKbart.value(), LigneKbartImprime.class));
            kbartAndImprimeDto.setNotice(service.getNoticeFromPpn(lignesKbart.value().getPpn().toString()));
            NoticeConcrete noticeElec = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
            //Ajout provider display name en 214 $c 2è occurrence
            String providerDisplay = baconService.getProviderDisplayName(provider);
            if (providerDisplay != null) {
                noticeElec.getNoticeBiblio().findZone("214", 1).addSubLabel("$c", providerDisplay);
            }
            String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
            service.addNoticeBouquetInBestPpn(noticeElec.getNoticeBiblio(), ppnNoticeBouquet);
            service.creerNotice(noticeElec);
            log.debug("Création notice à partir de l'imprimée terminée");
        } catch (CBSException | ZoneException e) {
            log.error(e.getMessage());
            emailService.sendErrorMailImprime(filename, lignesKbart.value(), e);
        } finally {
            service.disconnect();
        }
    }

    private String getFileNameFromHeader(Headers headers) {
        String filename = "";
        for (Header header : headers.toArray()) {
            if (header.key().equals("filename")) {
                filename = new String(header.value());
                break;
            }
        }
        return filename;
    }
}
