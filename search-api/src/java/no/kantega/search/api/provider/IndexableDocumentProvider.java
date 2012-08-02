package no.kantega.search.api.provider;

import no.kantega.search.api.IndexableDocument;
import no.kantega.search.api.index.ProgressReporter;

import java.util.concurrent.BlockingQueue;

/**
 * A provider of IndexableDocuments. Typically provides all the documents
 * of a particular type that should be indexed.
 */
public interface IndexableDocumentProvider {

    /**
     * @param indexableDocumentQueue - The queue this provider should put documents on to submit them for indexing.
     * @param numberOfThreadsToUse - How many threads the provider should use when putting documents on the queue.
     * @return the ProgressReporter that the provider reports progress to as it puts documents on the queue.
     */
    public ProgressReporter provideDocuments(BlockingQueue<IndexableDocument> indexableDocumentQueue, int numberOfThreadsToUse);

}
