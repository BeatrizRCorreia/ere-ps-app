package health.ere.ps.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;

public class XmlPrescriptionProcessor {
    static FhirContext fhirContext = FhirContext.forR4();

    // Get <Bundle> tag including content
    private static Pattern p = Pattern.compile(".*(<Bundle[^>]*>.*</Bundle>).*", Pattern.DOTALL);

    public static Bundle[] parseFromString(String xml) {

        Matcher m = p.matcher(xml);
        if (!m.matches()) {
            throw new WebApplicationException("Could not extract inner text", Status.NOT_ACCEPTABLE);
        } else {
            String bundleXml = m.group(1);
            return new Bundle [] {fhirContext.newXmlParser().parseResource(Bundle.class, bundleXml)};
        }
    }
}