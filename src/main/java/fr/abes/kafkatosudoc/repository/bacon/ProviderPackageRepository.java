package fr.abes.kafkatosudoc.repository.bacon;

import fr.abes.kafkatosudoc.configuration.BaconDbConfiguration;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackage;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
@BaconDbConfiguration
public interface ProviderPackageRepository extends JpaRepository<ProviderPackage, ProviderPackageId> {
    Optional<List<ProviderPackage>> findAllByProviderPackageId_DatePAndProviderPackageId_ProviderIdtProviderAndProviderPackageId_PackageName(Date date, Integer idProvider, String packageName);

    Optional<Integer> findProviderPackageByProviderPackageIdBefore(Integer number);
}
