package uk.ac.ebi.biosamples.solr.model.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;

import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.facet.FacetType;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

public class RegularFacetFetchStrategy implements FacetFetchStrategy {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public List<Optional<Facet>> fetchFacetsUsing(SolrSampleRepository solrSampleRepository,
                                      FacetQuery query,
                                      List<Entry<SolrSampleField, Long>> facetFieldCountEntries,
                                      Pageable facetPageable) {

        List<String> facetFieldNames = facetFieldCountEntries.stream()
                .map(Entry::getKey)
                .map(SolrSampleField::getSolrDocumentFieldName)
                .collect(Collectors.toList());

        FacetPage<?> facetPage = solrSampleRepository.getFacets(query, facetFieldNames, facetPageable);

        List<Optional<Facet>> facetResults = new ArrayList<>();
        for (Field field : facetPage.getFacetFields()) {

            // Get the field info associated with returned field from solr
            Optional<Entry<SolrSampleField, Long>> optionalFieldInfo = facetFieldCountEntries.stream()
                .filter(entry -> entry.getKey().getSolrDocumentFieldName().equals(field.getName()))
                .findFirst();

            if (!optionalFieldInfo.isPresent()) {
                throw new RuntimeException("Unexpected field returned when getting facets for "+facetFieldCountEntries);
            }

            Entry<SolrSampleField, Long> fieldCountEntry = optionalFieldInfo.get();

            // Create the list of facet value-count for the returned field
            List<LabelCountEntry> listFacetContent = new ArrayList<>();
            for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
                log.trace("Adding "+ fieldCountEntry.getKey().getLabel() +" : "+ffe.getValue()+" with count "+ffe.getValueCount());
                listFacetContent.add(LabelCountEntry.build(ffe.getValue(), ffe.getValueCount()));
            }

            // Build the facet
            SolrSampleField solrSampleField = fieldCountEntry.getKey();
            Optional<FacetType> associatedFacetType = solrSampleField.getSolrFieldType().getFacetFilterFieldType().getFacetType();
            if(associatedFacetType.isPresent()) {
                FacetType facetType = associatedFacetType.get();
                String facetLabel = solrSampleField.getLabel();
                Long facetCount = fieldCountEntry.getValue();
                Facet facet = facetType
                        .getBuilderForLabelAndCount(facetLabel, facetCount)
                        .withContent(new LabelCountListContent(listFacetContent))
                        .build();
                facetResults.add(Optional.of(facet));
            }
        }

        return facetResults;
    }
}
