package gov.nist.toolkit.fhir.utility;

import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.Map;

public interface IFhirSearch {
    Map<String, IBaseResource> search(String base, String resourceType, List params);
}