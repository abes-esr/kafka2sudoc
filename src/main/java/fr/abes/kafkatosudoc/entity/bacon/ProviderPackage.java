package fr.abes.kafkatosudoc.entity.bacon;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "PROVIDER_PACKAGE")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProviderPackage implements Serializable, Comparable<ProviderPackage> {
    @EmbeddedId
    private ProviderPackageId providerPackageId;

    @Column(name = "LABEL_ABES")
    private char labelAbes;

    @ManyToOne
    @JoinColumn(referencedColumnName = "IDT_PROVIDER", insertable = false, updatable = false)
    private Provider provider;

    public ProviderPackage(ProviderPackageId providerPackageId, char labelAbes) {
        this.providerPackageId = providerPackageId;
        this.labelAbes = labelAbes;
    }

    @Override
    public int compareTo(ProviderPackage o) {
        return this.providerPackageId.getDateP().compareTo(o.getProviderPackageId().getDateP());
    }
}
