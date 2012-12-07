package no.kantega.publishing.api.path;

import no.kantega.publishing.api.content.ContentIdentifier;

import java.util.List;

public interface PathEntryAO {

    /**
     * @param contentIdentifier -
     * @return The PathEntries on the path to the Content identified by ContentIdentifier
     */
    public List<PathEntry> getPathEntriesByContentIdentifier(ContentIdentifier contentIdentifier);

}
