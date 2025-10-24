package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.cbs.process.ProcessCBS;
import fr.abes.kafkatosudoc.exception.CommException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class SudocService {
	private final ProcessCBS cbs;

	public SudocService() {
		this.cbs = new ProcessCBS();
	}

	public SudocService(ProcessCBS cbs) {
		this.cbs = cbs;
	}


	/**
	 * méthode d'authentification au Sudoc : on vérifie avant qu'une connexion n'est pas déjà établie
	 * @param serveurSudoc nom du serveur
	 * @param portSudoc port de connexion
	 * @param loginSudoc login d'authentification
	 * @param passwordSudoc mot de passe
	 * @throws CBSException erreur d'authentification
	 * @throws IOException erreur de communication avec le CBS
	 */
	@Retryable(maxAttempts = 4, retryFor = IOException.class, noRetryFor = {CBSException.class}, backoff = @Backoff(delay = 1000, multiplier = 2) )
	public void authenticate(String serveurSudoc, String portSudoc, String loginSudoc, String passwordSudoc) throws CBSException, IOException {
		if (isNotLogged())
			try {
				this.cbs.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
			} catch (IOException ex) {
				this.cbs.disconnect();
				this.cbs.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
				throw ex;
			}
	}

	/**
	 * Méthode d'authentification sur une base Signal
	 * @param serveurSudoc nom du serveur
	 * @param portSudoc port de connexion
	 * @param loginSudoc login d'authentification
	 * @param passwordSudoc mot de passe
	 * @param signalDB numéro de la base signal
	 * @throws CBSException erreur d'authentification
	 * @throws IOException erreur de communication avec le CBS
	 */
	@Retryable(maxAttempts = 4, retryFor = IOException.class, noRetryFor = {CBSException.class}, backoff = @Backoff(delay = 1000, multiplier = 2) )
	public void authenticateBaseSignal(String serveurSudoc, String portSudoc, String loginSudoc, String passwordSudoc, String signalDB) throws CBSException, IOException {
		if (isNotLogged()) {
			try {
				this.cbs.authenticateWithLogicalDb(serveurSudoc, portSudoc, loginSudoc, passwordSudoc, signalDB);
			} catch (IOException ex) {
				this.cbs.disconnect();
				this.cbs.authenticateWithLogicalDb(serveurSudoc, portSudoc, loginSudoc, passwordSudoc, signalDB);
				throw ex;
			}
		}
	}

	/**
	 * Méthode vérifiant si l'instance CBS est déjà connectée au Sudoc
	 *
	 * @return true si authentifié
	 */
	private boolean isNotLogged() {
		return !this.cbs.getClientCBS().isLogged();
	}

	/**
	 * Méthode récupérant une notice bouquet en fonction d'un nom de package et d'un provider
	 *
	 * @param provider : nom du provider
	 * @param packageKbart : nom du package kbart
	 * @return le ppn de la notice récupérée
	 * @throws CBSException erreur dans la recherche de la notice bouquet
	 */
	public String getNoticeBouquet(String provider, String packageKbart) throws CBSException, IOException {
		String query = "che bou " + provider + "_" + packageKbart;
		log.debug("Recherche notice bouquet : " + query);
		this.cbs.search(query);
		if (this.cbs.getNbNotices() == 1) {
			return cbs.getPpnEncours();
		} else {
			throw new CBSException(Level.WARN, "Provider : " + provider + " / package : " + packageKbart + " : Recherche de la notice bouquet échouée");
		}
	}

	/**
	 * Méthode de recherche d'une notice par son ppn
	 *
	 * @param ppn ppn de la notice à chercher
	 * @return la notice trouvée mappée en objet
	 * @throws CBSException  : Aucune notice ne correspondant au ppn
	 * @throws ZoneException : erreur de construction de l'objet NoticeConcrete
	 */
	public NoticeConcrete getNoticeFromPpn(String ppn) throws CBSException, ZoneException, IOException {
		String query = "che ppn " + ppn;
		log.debug("Recherche notice : " + query);
		this.cbs.search(query);
		NoticeConcrete notice;
		if (this.cbs.getNbNotices() == 1) {
			notice = cbs.editerNoticeConcrete("1");
			this.cbs.back();
		} else {
			throw new CBSException(Level.WARN, "Aucune notice ne correspond à la recherche sur le PPN " + ppn);
		}
		return notice;
	}

	/**
	 * Méthode vérifiant si une notice bibliographique contient un lien vers une notice bouquet
	 *
	 * @param notice           notice à examiner
	 * @param ppnNoticeBouquet ppn de la notice bouquet à chercher dans la notice bibliographique
	 * @return true si un lien vers la notice bouquet est trouvé dans la notice bibliographique
	 */
	public boolean isNoticeBouquetInPpn(Biblio notice, String ppnNoticeBouquet) {
		return !notice.findZoneWithPattern("469", "$0", ppnNoticeBouquet).isEmpty();
	}

	/**
	 * Méthode d'ajout d'un lien vers une notice bouquet dans une notice bibliographique
	 *
	 * @param notice           notice à laquelle ajouter un lien vers la notice bouquet
	 * @param ppnNoticeBouquet ppn de la notice bouquet à lier à la notice bibliographique
	 * @throws ZoneException erreur de construction de la notice
	 */
	public void addNoticeBouquetInPpn(Biblio notice, String ppnNoticeBouquet) throws ZoneException {
		notice.addZone("469", "$0", ppnNoticeBouquet);
	}

	/**
	 * Méthode d'ajout du libellé d'une notice bouquet dans une notice bibliographique
	 *
	 * @param notice         notice à laquelle ajouter un lien vers la notice bouquet
	 * @param libelleBouquet ppn de la notice bouquet à lier à la notice bibliographique
	 * @throws ZoneException erreur de construction de la notice
	 */
	public void addLibelleNoticeBouquetInPpn(Biblio notice, String libelleBouquet) throws ZoneException {
		notice.addZone("469", "$s", libelleBouquet);
	}

	/**
	 * Passage en édition d'une noticeConcrete. ne peut être appelée qu'après une recherche
	 *
	 * @param noNotice numéro de la notice dans le lot récupéré via une recherche antérieure
	 * @return la notice en mode édition
	 * @throws ZoneException erreur de construction de la notice
	 * @throws CBSException  erreur lors du passage en édition
	 */
	public NoticeConcrete passageEditionNotice(int noNotice) throws ZoneException, CBSException, IOException {
		return cbs.editerNoticeConcrete(String.valueOf(noNotice));
	}

	/**
	 * Méthode de sauvegarde intégrale d'une notice (passage en édition + validation). ne peut être appelée qu'après une recherche
	 *
	 * @param notice  notice à sauvegarder
	 * @param noLigne numéro de notice dans le lot récupéré via une recherche antérieure
	 * @throws CBSException erreur de modification de la notice
	 */
	public void modifierNotice(NoticeConcrete notice, int noLigne) throws CBSException, IOException {
		this.cbs.modifierNoticeConcrete(String.valueOf(noLigne), notice);
	}

	/**
	 * Méthode de création d'une notice exnihilo
	 *
	 * @param notice notice à créer
	 * @throws CBSException Erreur de création de notice
	 */
	public void creerNotice(NoticeConcrete notice) throws CBSException, IOException {
		this.cbs.enregistrerNew(notice.toString());
	}

	/**
	 * Méthode de suppression d'un lien vers une notice bouquet dans une notice bibliographique
	 *
	 * @param notice           notice à modifier
	 * @param ppnNoticeBouquet ppn de la notice bouquet à supprimer de la notice bibliographique
	 * @return la notice après suppresion du lien vers la notice bouquet
	 */
	public Biblio supprimeNoticeBouquetInPpn(Biblio notice, String ppnNoticeBouquet) {
		if (notice.findZones("469").stream().anyMatch(zone -> (zone.findSubLabel("$0") != null && zone.findSubLabel("$0").equals(ppnNoticeBouquet)))) {
			notice.deleteZoneWithValue("469", "$0", ppnNoticeBouquet);
		}
		return notice;
	}

	/**
	 * Méthode permettant de lister les notices liées à la notice courante
	 *
	 * @return le nombre de notices liées
	 * @throws CBSException erreur sur la commande CBS
	 */
	public int getNoticesLiees() throws CBSException, IOException {
		return this.cbs.rel();
	}

	/**
	 * Méthode permettant de faire un retour arrière sur la dernière commande lancée dans le Sudoc
	 *
	 * @throws CBSException erreur sur la commande CBS
	 */
	public void retourArriere() throws CBSException, IOException {
		this.cbs.back();
	}

	/**
	 * Méthode permettant d'afficher le détail d'une notice dans un lot
	 *
	 * @param pos la position de la notice dans le lot
	 * @throws CBSException erreur sur la commande CBS
	 */
	public void voirNotice(int pos) throws CBSException, IOException {
		this.cbs.view(String.valueOf(pos), false, "UNM");
	}

	public void disconnect() throws CBSException {
		this.cbs.disconnect();
	}

	/**
	 * Méthode de déconnexion / reconnexion au sudoc et requalification de l'IOException
	 * @param serveurSudoc nom du serveur
	 * @param portSudoc port de connexion
	 * @param loginSudoc login d'authentification
	 * @param passwordSudoc mot de passe
	 * @param ex IOException à requalifier
	 * @throws CommException exception requalifiée
	 */
	public void decoRecoCbs(String serveurSudoc, String portSudoc, String loginSudoc, String passwordSudoc, Exception ex) throws CommException {
		try {
			log.warn("erreur de communication avec le Sudoc, tentative de reconnexion");
			this.disconnect();
			this.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
		} catch (CBSException | IOException e) {
			log.error(e.getMessage());
		}
		//on requalifie l'exception pour ne pas qu'elle soit confondue avec l'erreur d'envoi du mail
		throw new CommException(ex);
	}

    public Set<String> getListPpnSudoc(String serveurSudoc, String portSudoc, String loginSudoc, String passwordSudoc, String provider, String packageName) {
        Set<String> ppns = new HashSet<>();
        log.debug("Entré dans getListPpnSudoc");
        try {
            this.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
            String ppnBouquet = this.getNoticeBouquet(provider, packageName);
            log.debug("ppn bouquet : {}", ppnBouquet);
            int size = this.getNoticesLiees();
            ppns.addAll(this.cbs.getPpnsFromResultList(size));

            ppns.remove(ppnBouquet);
        } catch (IOException | CBSException e) {
            log.error(e.getMessage());
        }
        return ppns;
    }
}

