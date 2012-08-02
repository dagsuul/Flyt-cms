package no.kantega.search.api.index;

import no.kantega.search.api.IndexableDocument;

import java.util.List;

public interface DocumentIndexer {
    /**
     * Submit a document for indexing.
     * Do not force commit when the document is submitted.
     * @param document to be indexed
     */
    public void indexDocument(IndexableDocument document);

    /**
     * Submit a document for indexing.
     * Force commit when the document is submitted.
     * @param document to be indexed
     */
    public void indexDocumentAndCommit(IndexableDocument document);

    /**
     * Force a commit of the index.
     */
    public void commit();

    /**
     * Delete documents identified by the given ids
     * @param Ids of the documents to be deleted.
     */
    public void deleteById(List<String> Ids);

    /**
     * Force a optimization of the index.
     */
    public void optimize();
}
