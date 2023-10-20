package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.entity.LigneKbart;
import fr.abes.kafkatosudoc.entity.Provider;
import fr.abes.kafkatosudoc.entity.ProviderPackage;
import fr.abes.kafkatosudoc.repository.LigneKbartRepository;
import fr.abes.kafkatosudoc.repository.ProviderPackageRepository;
import fr.abes.kafkatosudoc.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BaconService {
    private final ProviderPackageRepository providerPackageRepository;
    private final ProviderRepository providerRepository;
    private final LigneKbartRepository ligneKbartRepository;


    public BaconService(ProviderPackageRepository providerPackageRepository, ProviderRepository providerRepository, LigneKbartRepository ligneKbartRepository) {
        this.providerPackageRepository = providerPackageRepository;
        this.providerRepository = providerRepository;
        this.ligneKbartRepository = ligneKbartRepository;
    }

    public ProviderPackage findLastVersionOfPackage(PackageKbartDto packageKbartDto) {
        Optional<Provider> providerOpt = providerRepository.findByProvider(packageKbartDto.getProvider());
        ProviderPackage providerPackage = null;
        if (providerOpt.isPresent()) {
            List<ProviderPackage> providerPackageList = providerPackageRepository.findAllByProviderPackageId_ProviderIdtProviderAndProviderPackageId_PackageName(providerOpt.get().getIdtProvider(), packageKbartDto.getPackageName());
            if (!providerPackageList.isEmpty()) {
                Collections.sort(providerPackageList);
                //suppression du premier élément de la liste correspondant au package courant inséré dans la base au moment du calcul du best ppn
                providerPackageList.remove(providerPackageList.size() - 1);
                for (ProviderPackage providerPackage1 : providerPackageList) {
                    if (providerPackage1.getProviderPackageId().getDateP().after(packageKbartDto.getDatePackage())) {
                        break;
                    }
                    providerPackage = providerPackage1;
                }
                return providerPackage;
            }
        }
        return null;
    }

    public Set<LigneKbart> findAllPpnFromPackage(ProviderPackage providerPackage) {
        return new HashSet<>(ligneKbartRepository.findAllByProviderPackage(providerPackage).stream().filter(ligneKbart -> ligneKbart.getBestPpn() != null).toList());
    }

    public String getProviderDisplayName(String provider) {
        Optional<Provider> providerOpt = providerRepository.findByProvider(provider);
        return providerOpt.map(Provider::getDisplayName).orElse(null);
    }
}
