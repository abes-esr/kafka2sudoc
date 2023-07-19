package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.process.ProcessCBS;
import lombok.Getter;
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

	@Getter
	private ProcessCBS cbs;

	public SudocService() {
	    cbs = new ProcessCBS();
    }

	public void authenticate(String login) throws CBSException {
		this.cbs.authenticate(serveurSudoc, portSudoc, login, passwordSudoc);
	}

	/**
	 * Deconnexion du client CBS (sudoc)
	 */
    public void disconnect() throws CBSException {
	    cbs.getClientCBS().disconnect();
    }
}
