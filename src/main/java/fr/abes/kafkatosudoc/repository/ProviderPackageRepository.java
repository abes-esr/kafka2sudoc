package fr.abes.kafkatosudoc.repository;

import fr.abes.kafkatosudoc.configuration.BaconDbConfiguration;
import fr.abes.kafkatosudoc.entity.ProviderPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@BaconDbConfiguration
public interface ProviderPackageRepository extends JpaRepository<ProviderPackage, Integer> {
    List<ProviderPackage> findAllByProviderIdtProviderAndPackageName(Integer idProvider, String packageName);

}
