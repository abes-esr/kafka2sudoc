package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.Exemplaire;
import fr.abes.cbs.notices.FORMATS;
import fr.abes.cbs.process.ProcessCBS;
import fr.abes.cbs.utilitaire.Constants;
import fr.abes.cbs.utilitaire.Utilitaire;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ProcessCBS cbs;


	public void authenticate() throws CBSException {
		this.cbs.authenticate(serveurSudoc, portSudoc, loginSudoc, passwordSudoc);
	}

	/**
	 * Deconnexion du client CBS (sudoc)
	 */
    public void disconnect() throws CBSException {
	    cbs.getClientCBS().disconnect();
    }

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

	public Biblio getNoticeFromPpn(String ppn) throws CBSException, ZoneException {
		String query = "che ppn " + ppn;
		log.debug("Recherche notice : " + query);
		this.cbs.search(query);
		if (this.cbs.getNbNotices() == 1) {
			this.cbs.affUnma();
			String result = cbs.editer("1");
			this.cbs.back();
			return new Biblio(result);
		} else {
			throw new CBSException("V/VERROR", "Aucune notice ne correspond à la recherche sur le PPN " + ppn);
		}
	}

	public boolean isNoticeBouquetInBestPpn(Biblio notice, String ppnNoticeBouquet) {
		return !notice.findZoneWithPattern("469", "0", ppnNoticeBouquet).isEmpty();
	}

	public Biblio addNoticeBouquetInBestPpn(Biblio notice, String ppnNoticeBouquet) throws ZoneException {
		notice.addZone("469", "$0", ppnNoticeBouquet);
		return notice;
	}

	public void sauvegarderNotice(Biblio noticeBestPpn) throws CBSException {
		this.cbs.modifierNotice("1", noticeBestPpn.toString());
	}
}
