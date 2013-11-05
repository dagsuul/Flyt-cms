package no.kantega.openaksess.search.controller;

import no.kantega.openaksess.search.query.AksessSearchContextCreator;
import no.kantega.openaksess.search.security.AksessSearchContext;
import no.kantega.publishing.api.content.ContentStatus;
import no.kantega.publishing.common.data.enums.ContentVisibilityStatus;
import no.kantega.publishing.controls.AksessController;
import no.kantega.search.api.search.QueryStringGenerator;
import no.kantega.search.api.search.SearchQuery;
import no.kantega.search.api.search.SearchResponse;
import no.kantega.search.api.search.Searcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Performs search for Aksess content.
 */
@Controller
public class ContentSearchController implements AksessController {
    @Autowired
    private Searcher searcher;

    @Autowired
    private AksessSearchContextCreator aksessSearchContextCreator;

    @Value("${oa.usefuzzysearch:false}")
    private boolean useFuzzySearch;

    private boolean searchAllSites = false;
    private boolean showOnlyVisibleContent = true;
    private boolean showOnlyPublishedContent = true;
    private List<String> facetFields = Collections.emptyList();
    private List<String> facetQueries = Collections.emptyList();

    /**
     * q - The query string. e.g. «cheese sale».
     * page - The result page wanted.
     * facetFields - The indexed fields to generate facet values from. e.g. «indexedContentType»
     * groupfield - Group results by the value they are having in this field.
     * resultsprpage - The number of results per page.
     * includeContentWithoutSite - if set to true, indexed content with siteId:-1 is included in search.
     * excludelinks - By default links for the paginating of results are generated. Set this parameter to true if this is not desired.
     * excludedefaultfacets - Faceting on location and documenttype is enabled by default. To disable this set this parameter to true.
     * filter - Additional filters may be added by setting this parameter. Each filter have to be on the format «field:value».
     * offset - To offset the results returned set the number of results to offset by in this parameter.
     */
    @RequestMapping("/search")
    public @ResponseBody Map<String, Object> handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> model = new HashMap<>();
        String query = getQuery(request);
        if (isNotEmpty(query)) {
            SearchResponse searchResponse = performSearch(request, query);
            model.put("searchResponse", searchResponse);

            if(includeLinks(request)){
                addLinks(model, searchResponse);
            }
        }

        return model;
    }

    /**
     *
     * @param request with the suggestion query in parameter «q» or «term».
     * @param limit - the number of suggestion wanted.
     * @return string suggestions matching the query.
     */
    @RequestMapping("/suggest")
    public @ResponseBody List<String> suggest(HttpServletRequest request, @RequestParam(required = false, defaultValue = "5") Integer limit) {
        String term = getQorTerm(request);
        SearchQuery query = new SearchQuery(aksessSearchContextCreator.getSearchContext(request), term);
        query.setResultsPerPage(limit);
        return searcher.suggest(query);
    }

    private String getQorTerm(HttpServletRequest request) {
        return ServletRequestUtils.getStringParameter(request, "q", request.getParameter("term"));
    }

    /**
     *
     * @param request with the spelling query in parameter «q» or «term».
     * @param limit - the number of spellings wanted.
     * @return strings with possible spellings.
     */
    @RequestMapping("/spelling")
    public @ResponseBody List<String> spelling(HttpServletRequest request, @RequestParam(required = false, defaultValue = "5") Integer limit) {
        String term = getQorTerm(request);
        SearchQuery query = new SearchQuery(aksessSearchContextCreator.getSearchContext(request), term);
        query.setResultsPerPage(limit);
        return searcher.spell(query);
    }

    private SearchResponse performSearch(HttpServletRequest request, String query) {
        AksessSearchContext searchContext = aksessSearchContextCreator.getSearchContext(request);
        return searcher.search(getSearchQuery(request, query, searchContext));
    }

    private SearchQuery getSearchQuery(HttpServletRequest request, String query, AksessSearchContext searchContext) {
        SearchQuery searchQuery = new SearchQuery(searchContext, query, getFilterQueries(request, searchContext));
        searchQuery.setFuzzySearch(useFuzzySearch);
        searchQuery.setPageNumber(ServletRequestUtils.getIntParameter(request, "page", 0));
        searchQuery.setResultsPerPage(ServletRequestUtils.getIntParameter(request, "resultsprpage", SearchQuery.DEFAULT_RESULTS_PER_PAGE));
        searchQuery.setOffset(ServletRequestUtils.getIntParameter(request, "offset", 0));

        addFacetFields(request, searchQuery);

        searchQuery.setGroupField(getGroupField(request));
        searchQuery.setGroupQueries(getGroupQueries(request));
        return searchQuery;
    }

    private void addFacetFields(HttpServletRequest request, SearchQuery searchQuery) {
        boolean excludeDefaultFacets = ServletRequestUtils.getBooleanParameter(request, "excludedefaultfacets", false);
        searchQuery.setFacetFields(getFacetFields(request, excludeDefaultFacets));

        if (!excludeDefaultFacets) {
            searchQuery.setFacetQueries(facetQueries);
        }
    }

    private String getGroupField(HttpServletRequest request) {
        return ServletRequestUtils.getStringParameter(request, "groupfield", null);
    }

    private String[] getGroupQueries(HttpServletRequest request) {
        return ServletRequestUtils.getStringParameters(request, "groupqueries");
    }

    private List<String> getFacetFields(HttpServletRequest request, boolean excludeDefaultFacets) {
        List<String> fields = excludeDefaultFacets? Collections.<String>emptyList() : facetFields;
        String parameterfacetFields = ServletRequestUtils.getStringParameter(request, "facetFields", null);
        if(parameterfacetFields != null){
            fields = Arrays.asList(parameterfacetFields.split(","));
        }
        return fields;
    }

    private List<String> getFilterQueries(HttpServletRequest request, AksessSearchContext searchContext) {
        List<String> filterQueries = new ArrayList<>(Arrays.asList(ServletRequestUtils.getStringParameters(request, QueryStringGenerator.FILTER_PARAM)));

        addSiteFilter(searchContext, filterQueries, ServletRequestUtils.getBooleanParameter(request, "includeContentWithoutSite", false));

        addVisibilityFilter(filterQueries);

        addPublishedFilter(filterQueries);

        return filterQueries;
    }

    private void addPublishedFilter(List<String> filterQueries) {
        String publishedContentFilter = "contentStatus:" + ContentStatus.PUBLISHED.name();
        if(!filterQueries.contains(publishedContentFilter) && showOnlyPublishedContent){
            filterQueries.add(publishedContentFilter);
        }
    }

    private void addVisibilityFilter(List<String> filterQueries) {
        String visibleContentFilter = "visibilityStatus:" + ContentVisibilityStatus.getName(ContentVisibilityStatus.ACTIVE);
        if(!filterQueries.contains(visibleContentFilter) && showOnlyVisibleContent){
            filterQueries.add(visibleContentFilter);
        }
    }

    private void addSiteFilter(AksessSearchContext searchContext, List<String> filterQueries, boolean includeContentWithoutSite) {
        StringBuilder siteFilterBuilder = new StringBuilder("siteId:");
        siteFilterBuilder.append(searchContext.getSiteId());
        if(includeContentWithoutSite){
            siteFilterBuilder.append(" OR siteId:\\-1");
        }
        String siteFilter = siteFilterBuilder.toString();
        if(!filterQueries.contains(siteFilter) && !searchAllSites){
            filterQueries.add(siteFilter);
        }
    }

    private String getQuery(HttpServletRequest request) {
        return ServletRequestUtils.getStringParameter(request, QueryStringGenerator.QUERY_PARAM, "");
    }

    private boolean includeLinks(HttpServletRequest request) {
        return !ServletRequestUtils.getBooleanParameter(request, "excludelinks", false);
    }

    private void addLinks(Map<String, Object> model, SearchResponse searchResponse) {
        Map<String, Object> links = new HashMap<>();
        model.put("links", links);
        int currentPage = searchResponse.getCurrentPage();
        if (currentPage > 0) {
            String prevPageUrl = QueryStringGenerator.getPrevPageUrl(searchResponse.getQuery(), currentPage);
            links.put("prevPageUrl", prevPageUrl);
        }

        int numberOfPages = searchResponse.getNumberOfPages();
        if (currentPage < (numberOfPages - 1)) {
            String nextPageUrl = QueryStringGenerator.getNextPageUrl(searchResponse.getQuery(), currentPage);
            links.put("nextPageUrl", nextPageUrl);
        }

        if (numberOfPages > 1) {
            links.put("pageUrls", QueryStringGenerator.getPageUrls(searchResponse, currentPage));
        }
    }

    public String getDescription() {
        return "Performs search for Aksess content";
    }

    public void setSearchAllSites(boolean searchAllSites) {
        this.searchAllSites = searchAllSites;
    }

    public void setShowOnlyVisibleContent(boolean showOnlyVisibleContent) {
        this.showOnlyVisibleContent = showOnlyVisibleContent;
    }

    public void setShowOnlyPublishedContent(boolean showOnlyPublishedContent) {
        this.showOnlyPublishedContent = showOnlyPublishedContent;
    }

    public void setFacetFields(List<String> facetFields) {
        this.facetFields = facetFields;
    }

    public void setFacetQueries(List<String> facetQueries) {
        this.facetQueries = facetQueries;
    }
}
