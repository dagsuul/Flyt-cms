package no.kantega.openaksess.search.provider.result;

import no.kantega.openaksess.search.provider.transformer.AttachmentTransformer;
import no.kantega.openaksess.search.provider.transformer.ContentTransformer;
import no.kantega.publishing.api.path.PathEntryService;
import no.kantega.search.api.provider.SearchResultDecorator;
import no.kantega.search.api.search.SearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static no.kantega.search.api.util.FieldUtils.getLanguageSuffix;

@Component
public class OASearchResultDecorator implements SearchResultDecorator<OASearchResult> {

    @Autowired
    private PathEntryService pathEntryService;

    @Override
    public Collection<String> handledindexedContentTypes() {
        return Arrays.asList(AttachmentTransformer.HANDLED_DOCUMENT_TYPE, ContentTransformer.HANDLED_DOCUMENT_TYPE);
    }

    @Override
    public OASearchResult decorate(Map<String, Object> resultMap, String description, SearchQuery query) {
        String language = (String) resultMap.get("language");
        String languageSuffix = getLanguageSuffix(language);
        Integer parentId = (Integer) resultMap.get("parentId");

        return new OASearchResult((Integer) resultMap.get("id"),
                (Integer) resultMap.get("securityId"),
                (String) resultMap.get("indexedContentType"),
                (String) resultMap.get("title_" + languageSuffix),
                description,
                (String) resultMap.get("author"),
                (String) resultMap.get("url"),
                parentId, pathEntryService.getPathEntriesByAssociationIdInclusive(parentId));
    }
}
