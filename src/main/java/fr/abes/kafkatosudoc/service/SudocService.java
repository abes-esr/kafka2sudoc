package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.cbs.process.ProcessCBS;
import lombok.extern.slf4j.Slf4j;
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

	public boolean isNoticeBouquetInPpn(Biblio notice, String ppnNoticeBouquet) {
		return !notice.findZoneWithPattern("469", "$0", ppnNoticeBouquet).isEmpty();
	}

	public Biblio addNoticeBouquetInPpn(Biblio notice, String ppnNoticeBouquet) throws ZoneException {
		notice.addZone("469", "$0", ppnNoticeBouquet);
		return notice;
	}

	public NoticeConcrete passageEditionNotice(int noNotice) throws ZoneException, CBSException {
		return cbs.editerNoticeConcrete(String.valueOf(noNotice));
	}
	public void modifierNotice(NoticeConcrete noticeBestPpn, int noLigne) throws CBSException {
		this.cbs.modifierNoticeConcrete(String.valueOf(noLigne), noticeBestPpn);
	}

	public void creerNotice(NoticeConcrete notice) throws CBSException {
		this.cbs.enregistrerNew(notice.toString());
	}

	public Biblio supprimeNoticeBouquetInPpn(Biblio notice, String ppnNoticeBouquet) {
		notice.deleteZoneWithValue("469", "$0", ppnNoticeBouquet);
		return notice;
	}

	public int getNoticesLiees() throws CBSException {
		return this.cbs.rel();
	}

	public void retourArriere() throws CBSException {
		this.cbs.back();
	}

	public void voirNotice(int pos) throws CBSException {
		this.cbs.view(String.valueOf(pos), false, "UNM");
	}
}
