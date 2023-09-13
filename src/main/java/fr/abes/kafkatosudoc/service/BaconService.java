package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.entity.bacon.Provider;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackage;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackageId;
import fr.abes.kafkatosudoc.repository.bacon.LigneKbartRepository;
import fr.abes.kafkatosudoc.repository.bacon.ProviderPackageRepository;
import fr.abes.kafkatosudoc.repository.bacon.ProviderRepository;
import fr.abes.kafkatosudoc.utils.UtilsMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BaconService {
    private ProviderPackageRepository providerPackageRepository;
    private ProviderRepository providerRepository;
    private LigneKbartRepository ligneKbartRepository;
    private UtilsMapper mapper;

    public BaconService(ProviderPackageRepository providerPackageRepository, ProviderRepository providerRepository, LigneKbartRepository ligneKbartRepository, UtilsMapper mapper) {
        this.providerPackageRepository = providerPackageRepository;
        this.providerRepository = providerRepository;
        this.ligneKbartRepository = ligneKbartRepository;
        this.mapper = mapper;
    }

    public Optional<List<ProviderPackage>> findLastVersionOfPackage(PackageKbartDto packageKbartDto){
        Optional<Provider> providerOpt = providerRepository.findByProvider(packageKbartDto.getProvider());
        if (providerOpt.isPresent()) {
            return providerPackageRepository.findAllByProviderPackageId_DatePAndProviderPackageId_ProviderIdtProviderAndProviderPackageId_PackageName(packageKbartDto.getDatePackage(), providerOpt.get().getIdtProvider(), packageKbartDto.getPackageName());
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param listProviderPackage toutes les lignes en base de donnée bacon avec un provider et un package identique
     * @param datePackage la date figurant dans l'entête du fichier kbart
     * @return vrai si il existe une version avec une date antérieure à celle du fichier que l'on est en train de traiter
     * La date spécifiée dans le fichier est censée etre toujours la date du jour (puisque celle de l'envoi)
     */
    public boolean isAnteriorVersionExist(Optional<List<ProviderPackage>> listProviderPackage, Date datePackage){
        System.out.println("cdc");
        List<ProviderPackage> listProviderPackageLocal = listProviderPackage.orElse(new ArrayList<>());
            Collections.sort(listProviderPackageLocal);
            if(!listProviderPackageLocal.isEmpty()){
                return listProviderPackageLocal.get(0).getProviderPackageId().getDateP().getTime() <= datePackage.getTime();
            }else{
                return false;
            }
    }
}
