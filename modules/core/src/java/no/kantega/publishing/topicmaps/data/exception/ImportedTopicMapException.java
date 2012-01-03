package no.kantega.publishing.topicmaps.data.exception;

public class ImportedTopicMapException extends Exception{

    public ImportedTopicMapException(String message) {
        super(message);
    }

    public ImportedTopicMapException(String message, Throwable cause) {
        super(message, cause);
    }
}
