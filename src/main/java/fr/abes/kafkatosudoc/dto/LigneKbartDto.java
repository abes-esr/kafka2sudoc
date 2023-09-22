package fr.abes.kafkatosudoc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

@Data
@NoArgsConstructor
public class LigneKbartDto {
    @JsonProperty(value = "publication_title")
    private String publicationTitle;
    @JsonProperty(value = "print_identifier")
    private String printIdentifier;
    @JsonProperty(value = "online_identifier")
    private String onlineIdentifier;
    @JsonProperty(value = "date_first_issue_online")
    private String dateFirstIssueOnline;
    @JsonProperty(value = "num_first_vol_online")
    private Integer numFirstVolOnline;
    @JsonProperty(value = "num_first_issue_online")
    private Integer numFirstIssueOnline;
    @JsonProperty(value = "date_last_issue_online")
    private String dateLastIssueOnline;
    @JsonProperty(value = "num_last_vol_online")
    private Integer numLastVolOnline;
    @JsonProperty(value = "num_last_issue_online")
    private Integer numLastIssueOnline;
    @JsonProperty(value = "title_url")
    private String titleUrl;
    @JsonProperty(value = "first_author")
    private String firstAuthor;
    @JsonProperty(value = "title_id")
    private String titleId;
    @JsonProperty(value = "embargo_info")
    private String embargoInfo;
    @JsonProperty(value = "coverage_depth")
    private String coverageDepth;
    @JsonProperty(value = "notes")
    private String notes;
    @JsonProperty(value = "publisher_name")
    private String publisherName;
    @JsonProperty(value = "publication_type")
    private String publicationType;
    @JsonProperty(value = "date_monograph_published_print")
    private String dateMonographPublishedPrint;
    @JsonProperty(value = "date_monograph_published_online")
    private String dateMonographPublishedOnline;
    @JsonProperty(value = "monograph_volume")
    private Integer monographVolume;
    @JsonProperty(value = "monograph_edition")
    private String monographEdition;
    @JsonProperty(value = "first_editor")
    private String firstEditor;
    @JsonProperty(value = "parent_publication_title_id")
    private String parentPublicationTitleId;
    @JsonProperty(value = "preceding_publication_title_id")
    private String precedingPublicationTitleId;
    @JsonProperty(value = "access_type")
    private String accessType;
    @JsonProperty(value = "bestPpn")
    private String bestPpn;

    @Override
    public int hashCode() {
        return this.publicationTitle.hashCode() * this.onlineIdentifier.hashCode() * this.printIdentifier.hashCode();
    }

    public boolean isBestPpnEmpty() {
        return this.bestPpn == null || this.bestPpn.isEmpty();
    }

    @Override
    public String toString() {
        return this.publicationTitle + "\t"
                + (this.onlineIdentifier.isEmpty() ? "" : this.onlineIdentifier) + "\t"
                + (this.printIdentifier.isEmpty() ? "" :  this.printIdentifier) + "\t"
                + (this.dateFirstIssueOnline.isEmpty() ? "" : this.dateFirstIssueOnline) + "\t"
                + this.numFirstVolOnline + "\t"
                + this.numFirstIssueOnline + "\t"
                + (this.dateLastIssueOnline.isEmpty() ? "" : this.dateLastIssueOnline) + "\t"
                + this.numLastVolOnline + "\t"
                + this.numLastIssueOnline + "\t"
                + (this.titleUrl.isEmpty() ? "" : this.titleUrl) + "\t"
                + (this.firstAuthor.isEmpty() ? "" : this.firstAuthor) + "\t"
                + (this.titleId.isEmpty() ? "" : this.titleId) + "\t"
                + (this.embargoInfo.isEmpty() ? "" : this.embargoInfo) + "\t"
                + (this.coverageDepth.isEmpty() ? "" : this.coverageDepth) + "\t"
                + (this.notes.isEmpty() ? "" : this.notes) + "\t"
                + (this.publisherName.isEmpty() ? "" : this.publisherName) + "\t"
                + this.publicationType + "\t"
                + (this.dateMonographPublishedPrint.isEmpty() ? "" : this.dateMonographPublishedPrint) + "\t"
                + (this.dateMonographPublishedOnline.isEmpty() ? "" : this.dateMonographPublishedOnline) + "\t"
                + this.monographVolume + "\t"
                + (this.monographEdition.isEmpty() ? "" : this.monographEdition) + "\t"
                + (this.firstEditor.isEmpty() ? "" : this.firstEditor) + "\t"
                + (this.parentPublicationTitleId.isEmpty() ? "" : this.parentPublicationTitleId) + "\t"
                + (this.precedingPublicationTitleId.isEmpty() ? "" : this.precedingPublicationTitleId) + "\t"
                + (this.accessType.isEmpty() ? "" : this.accessType) + "\t"
                + (this.bestPpn == null || this.bestPpn.isEmpty() ? "" : this.bestPpn);
    }

}
