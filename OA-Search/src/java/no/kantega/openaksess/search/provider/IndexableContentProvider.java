package no.kantega.openaksess.search.provider;

import no.kantega.openaksess.search.provider.transformer.ContentTransformer;
import no.kantega.publishing.common.data.ContentIdentifier;
import no.kantega.publishing.common.service.ContentManagementService;
import no.kantega.publishing.security.SecuritySession;
import no.kantega.search.api.IndexableDocument;
import no.kantega.search.api.provider.IndexableDocumentProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

@Component
public class IndexableContentProvider implements IndexableDocumentProvider {

    @Autowired
    @Qualifier("aksessDataSource")
    private DataSource dataSource;

    @Autowired
    private ContentTransformer transformer;

    public Iterator<IndexableDocument> provideDocuments() {
        ContentManagementService contentManagementService = new ContentManagementService(SecuritySession.createNewAdminInstance());
        return new IndexableContentDocumentIterator(dataSource, contentManagementService);
    }

    public long getNumberOfDocuments() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.queryForInt("SELECT count(*) FROM content, associations WHERE content.IsSearchable = 1 AND content.ContentId = associations.ContentId AND associations.IsDeleted = 0");
    }

    private class IndexableContentDocumentIterator implements Iterator<IndexableDocument>{
        private final ResultSet resultSet;
        private final ContentManagementService cms;

        private IndexableContentDocumentIterator(DataSource dataSource, ContentManagementService cms) {
            this.cms = cms;
            try {
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT content.ContentId FROM content, associations WHERE content.IsSearchable = 1 AND content.ContentId = associations.ContentId AND associations.IsDeleted = 0");
                resultSet = preparedStatement.executeQuery();
            } catch (SQLException e) {
                throw new IllegalStateException("Could not connect to database", e);
            }
        }

        public boolean hasNext() {
            try {
                return resultSet.next();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        public IndexableDocument next() {
            try {
                ContentIdentifier contentIdentifier = new ContentIdentifier();
                contentIdentifier.setContentId(resultSet.getInt("ContentId"));
                return transformer.transform(cms.getContent(contentIdentifier));
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported");
        }
    }
}