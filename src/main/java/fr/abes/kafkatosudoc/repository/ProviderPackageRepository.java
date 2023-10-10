package fr.abes.kafkatosudoc.repository;

import fr.abes.kafkatosudoc.configuration.BaconDbConfiguration;
import fr.abes.kafkatosudoc.entity.ProviderPackage;
import fr.abes.kafkatosudoc.entity.ProviderPackageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@BaconDbConfiguration
public interface ProviderPackageRepository extends JpaRepository<ProviderPackage, ProviderPackageId> {
    List<ProviderPackage> findAllByProviderPackageId_ProviderIdtProviderAndProviderPackageId_PackageName(Integer idProvider, String packageName);

}
