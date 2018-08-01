package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghLocation;
import uk.ac.ebi.biosamples.service.GeoLocationDataHelper;

import static org.junit.Assert.*;

public class GeoLocationDataHelperTest {

    private GeoLocationDataHelper geoLocationDataHelper;

    public GeoLocationDataHelperTest(){
        geoLocationDataHelper = new GeoLocationDataHelper();
    }

    @Test
    public void geolocationDataIdentifyingTest(){
        assertTrue(geoLocationDataHelper.isGeoLocationData("geographic location"));
        assertTrue(geoLocationDataHelper.isGeoLocationData("location"));
        assertTrue(geoLocationDataHelper.isGeoLocationData("latitude"));
        assertTrue(geoLocationDataHelper.isGeoLocationData("longitude"));
        assertTrue(geoLocationDataHelper.isGeoLocationData("altitude"));
        assertTrue(geoLocationDataHelper.isGeoLocationData("precision"));
        assertFalse(geoLocationDataHelper.isGeoLocationData(""));
        assertFalse(geoLocationDataHelper.isGeoLocationData("fsdfsd"));
    }

    @Test
    public void convertationToDecimalDegreeTest(){
        String testLocation1 = "1 N 1 E";
        Ga4ghLocation expectedLocation1 = new Ga4ghLocation(1,1);
        assertEquals(expectedLocation1,geoLocationDataHelper.convertToDecimalDegree(testLocation1));

        String testLocation2 = "1 S 1 E";
        Ga4ghLocation expectedLocation2 = new Ga4ghLocation(-1,1);
        assertEquals(expectedLocation2,geoLocationDataHelper.convertToDecimalDegree(testLocation2));

        String testLocation3 = "1 S 1 W";
        Ga4ghLocation expectedLocation3 = new Ga4ghLocation(-1,-1);
        assertEquals(expectedLocation3,geoLocationDataHelper.convertToDecimalDegree(testLocation3));
    }
}