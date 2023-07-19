package fr.abes.kafkatosudoc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LigneKbartDto {

    private String publicationTitle;
    private String printIdentifier;
    private String onlineIdentifier;
    private String dateFirstIssueOnline;
    private Integer numFirstVolOnline;
    private Integer numFirstIssueOnline;
    private String dateLastIssueOnline;
    private Integer numLastVolOnline;
    private Integer numLastIssueOnline;
    private String titleUrl;
    private String firstAuthor;
    private String titleId;
    private String embargoInfo;
    private String coverageDepth;
    private String notes;
    private String publisherName;
    private String publicationType;
    private String dateMonographPublishedPrint;
    private String dateMonographPublishedOnline;
    private Integer monographVolume;
    private String monographEdition;
    private String firstEditor;
    private String parentPublicationTitleId;
    private String precedingPublicationTitleId;
    private String accessType;
    private String bestPpn;

    @Override
    public int hashCode() {
        return this.publicationTitle.hashCode() * this.onlineIdentifier.hashCode() * this.printIdentifier.hashCode();
    }

    @Override
    public String toString() {
        return "publication title : " + this.publicationTitle + " / publication_type : " + this.publicationType +
                (this.onlineIdentifier.isEmpty() ? "" : " / online_identifier : " + this.onlineIdentifier) +
                (this.printIdentifier.isEmpty() ? "" : " / print_identifier : " + this.printIdentifier);
    }

}
