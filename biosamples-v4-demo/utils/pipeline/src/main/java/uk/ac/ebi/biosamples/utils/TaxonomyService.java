package uk.ac.ebi.biosamples.utils;

import org.springframework.stereotype.Service;

@Service
public class TaxonomyService {

	public String getUriForTaxonId(int taxonId) {
		return "http://purl.obolibrary.org/obo/NCBITaxon_"+taxonId;
	}
}
