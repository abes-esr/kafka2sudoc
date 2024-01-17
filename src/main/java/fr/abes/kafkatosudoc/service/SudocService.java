package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.cbs.process.ProcessCBS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SudocService {

	@Value("${sudoc.serveur}")
	private String serveurSudoc;

	@Value("${sudoc.port}")
	private String portSudoc;

	@Value("${sudoc.password}")
	private String passwordSudoc;

	@Value("${sudoc.login}")
	private String loginSudoc;


	@Value("${sudoc.signalDb}")
	private String signalDB;

	private final ProcessCBS cbs;

	public SudocService(ProcessCBS cbs) {
		this.cbs = cbs;
	}


	/**
	 * méthode d'authentification au Sudoc : on vérifie avant qu'une connexion n'est pas déjà établie
	 *
	 * @throws CBSException erreur d'authentification
	 */
	public void authenticate() throws CBSException {
		if (!isLogged())
			this.cbs.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
	}

	public void authenticateBaseSignal() throws CBSException {
		if (!isLogged())
			this.cbs.authenticateWithLogicalDb(serveurSudoc, portSudoc, loginSudoc, passwordSudoc, signalDB);
	}

	/**
	 * Méthode vérifiant si l'instance CBS est déjà connectée au Sudoc
	 *
	 * @return true si authentifié
	 */
	private boolean isLogged() {
		return this.cbs.getClientCBS().isLogged();
	}

	/**
	 * Méthode récupérant une notice bouquet en fonction d'un nom de package et d'un provider
	 *
	 * @param provider
	 * @param packageKbart
	 * @return le ppn de la notice récupérée
	 * @throws CBSException erreur dans la recherche de la notice bouquet
	 */
	public String getNoticeBouquet(String provider, String packageKbart) throws CBSException {
		String query = "che bou " + provider + "_" + packageKbart;
		log.debug("Recherche notice bouquet : " + query);
		this.cbs.search(query);
		if (this.cbs.getNbNotices() == 1) {
			return cbs.getPpnEncours();
		} else {
			throw new CBSException("V/VERROR", "Provider : " + provider + " / package : " + packageKbart + " : Recherche de la notice bouquet échouée");
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
	public NoticeConcrete getNoticeFromPpn(String ppn) throws CBSException, ZoneException {
		String query = "che ppn " + ppn;
		log.debug("Recherche notice : " + query);
		this.cbs.search(query);
		NoticeConcrete notice;
		if (this.cbs.getNbNotices() == 1) {
			notice = cbs.editerNoticeConcrete("1");
			this.cbs.back();
		} else {
			throw new CBSException("V/VERROR", "Aucune notice ne correspond à la recherche sur le PPN " + ppn);
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
		notice.addZone("469", "$", libelleBouquet);
	}

	/**
	 * Passage en édition d'une noticeConcrete. ne peut être appelée qu'après une recherche
	 *
	 * @param noNotice numéro de la notice dans le lot récupéré via une recherche antérieure
	 * @return la notice en mode édition
	 * @throws ZoneException erreur de construction de la notice
	 * @throws CBSException  erreur lors du passage en édition
	 */
	public NoticeConcrete passageEditionNotice(int noNotice) throws ZoneException, CBSException {
		return cbs.editerNoticeConcrete(String.valueOf(noNotice));
	}

	/**
	 * Méthode de sauvegarde intégrale d'une notice (passage en édition + validation). ne peut être appelée qu'après une recherche
	 *
	 * @param notice  notice à sauvegarder
	 * @param noLigne numéro de notice dans le lot récupéré via une recherche antérieure
	 * @throws CBSException erreur de modification de la notice
	 */
	public void modifierNotice(NoticeConcrete notice, int noLigne) throws CBSException {
		this.cbs.modifierNoticeConcrete(String.valueOf(noLigne), notice);
	}

	/**
	 * Méthode de création d'une notice exnihilo
	 *
	 * @param notice notice à créer
	 * @throws CBSException Erreur de création de notice
	 */
	public void creerNotice(NoticeConcrete notice) throws CBSException {
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
		notice.deleteZoneWithValue("469", "$0", ppnNoticeBouquet);
		return notice;
	}

	/**
	 * Méthode permettant de lister les notices liées à la notice courante
	 *
	 * @return le nombre de notices liées
	 * @throws CBSException erreur sur la commande CBS
	 */
	public int getNoticesLiees() throws CBSException {
		return this.cbs.rel();
	}

	/**
	 * Méthode permettant de faire un retour arrière sur la dernière commande lancée dans le Sudoc
	 *
	 * @throws CBSException erreur sur la commande CBS
	 */
	public void retourArriere() throws CBSException {
		this.cbs.back();
	}

	/**
	 * Méthode permettant d'afficher le détail d'une notice dans un lot
	 *
	 * @param pos la position de la notice dans le lot
	 * @throws CBSException erreur sur la commande CBS
	 */
	public void voirNotice(int pos) throws CBSException {
		this.cbs.view(String.valueOf(pos), false, "UNM");
	}
}

