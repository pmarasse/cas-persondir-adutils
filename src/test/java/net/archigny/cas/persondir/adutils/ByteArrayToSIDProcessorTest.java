package net.archigny.cas.persondir.adutils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;

public class ByteArrayToSIDProcessorTest {

    public final static byte[]       TOKEN_1     = Base64.decodeBase64("AQIAAAAAAAUgAAAAIQIAAA==");

    public final static String       GROUP_1_SID = "S-1-5-32-545";

    public final static byte[]       TOKEN_2     = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlWQQAAA==");

    public final static String       GROUP_2_SID = "S-1-5-21-2790624211-2154948103-1704485230-1113";

    public final static byte[]       TOKEN_3     = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlWAQAAA==");

    public final static String       GROUP_3_SID = "S-1-5-21-2790624211-2154948103-1704485230-1112";

    public final static byte[]       TOKEN_4     = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlAQIAAA==");

    public final static String       GROUP_4_SID = "S-1-5-21-2790624211-2154948103-1704485230-513";

    public final static byte[]       TOKEN_5     = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlVwQAAA==");

    public final static String       GROUP_5_SID = "S-1-5-21-2790624211-2154948103-1704485230-1111";

    public final Logger              log         = LoggerFactory.getLogger(ByteArrayToSIDProcessorTest.class);

    public final static String       sourceAttr  = "tokenGroups";

    public final static String       destAttr    = "groupes-sid";

    public Map<String, List<Object>> attrs;

    @Before
    public void setUp() {

        attrs = new HashMap<String, List<Object>>(3);
        List<Object> tokenGroups = new ArrayList<Object>();
        tokenGroups.add(TOKEN_1);
        tokenGroups.add(TOKEN_2);
        tokenGroups.add(TOKEN_3);
        tokenGroups.add(TOKEN_4);
        tokenGroups.add(TOKEN_5);
        attrs.put(sourceAttr, tokenGroups);
    }

    @Test
    public void simpleReplaceTest() throws Exception {

        log.info("remplacement en lieu et place des SID binaires par leur équivalent chaîne");
        ByteArrayToSIDProcessor processor = new ByteArrayToSIDProcessor();
        try {
            processor.afterPropertiesSet();
            fail("without source attribute, afterPropertiesSet must fail");
        } catch (BeanCreationException e) {
        }
        processor.setSourceAttribute(sourceAttr);
        processor.afterPropertiesSet();
        processor.processAttributes(attrs);

        List<Object> groupesSid = attrs.get(sourceAttr);
        assertNotNull(groupesSid);
        assertTrue(groupesSid.contains(GROUP_1_SID));
        assertTrue(groupesSid.contains(GROUP_2_SID));
        assertTrue(groupesSid.contains(GROUP_3_SID));
        assertTrue(groupesSid.contains(GROUP_4_SID));
        assertTrue(groupesSid.contains(GROUP_5_SID));
        assertEquals(5, groupesSid.size());
    }

    @Test
    public void otherReplaceTest() throws Exception {

        log.info("Remplacement en lieu et place des SID binaires par leur équivalent chaîne en passant cible = source");
        ByteArrayToSIDProcessor processor = new ByteArrayToSIDProcessor();
        processor.setSourceAttribute(sourceAttr);
        processor.setTargetAttribute(sourceAttr);
        processor.afterPropertiesSet();
        processor.processAttributes(attrs);

        List<Object> groupesSid = attrs.get(sourceAttr);
        assertNotNull(groupesSid);
        assertTrue(groupesSid.contains(GROUP_1_SID));
        assertTrue(groupesSid.contains(GROUP_2_SID));
        assertTrue(groupesSid.contains(GROUP_3_SID));
        assertTrue(groupesSid.contains(GROUP_4_SID));
        assertTrue(groupesSid.contains(GROUP_5_SID));
        assertEquals(5, groupesSid.size());
    }

    @Test
    public void simpleTransferTest() throws Exception {

        log.info("Déplacement vers un autre attribut");

        ByteArrayToSIDProcessor processor = new ByteArrayToSIDProcessor();
        processor.setSourceAttribute(sourceAttr);
        processor.setTargetAttribute(destAttr);
        processor.afterPropertiesSet();
        processor.processAttributes(attrs);

        assertNull(attrs.get(sourceAttr));
        List<Object> groupesSid = attrs.get(destAttr);
        assertNotNull(groupesSid);
        assertTrue(groupesSid.contains(GROUP_1_SID));
        assertTrue(groupesSid.contains(GROUP_2_SID));
        assertTrue(groupesSid.contains(GROUP_3_SID));
        assertTrue(groupesSid.contains(GROUP_4_SID));
        assertTrue(groupesSid.contains(GROUP_5_SID));
        assertEquals(5, groupesSid.size());
    }

    @Test
    public void simpleCopyTest() throws Exception {

        log.info("Copie vers un autre attribut");

        ByteArrayToSIDProcessor processor = new ByteArrayToSIDProcessor();
        processor.setSourceAttribute(sourceAttr);
        processor.setTargetAttribute(destAttr);
        processor.setDeleteSourceAttribute(false);
        processor.afterPropertiesSet();
        processor.processAttributes(attrs);

        List<Object> tokenGroups = attrs.get(sourceAttr);
        assertNotNull(tokenGroups);
        assertTrue(tokenGroups.contains(TOKEN_1));
        assertTrue(tokenGroups.contains(TOKEN_2));
        assertTrue(tokenGroups.contains(TOKEN_3));
        assertTrue(tokenGroups.contains(TOKEN_4));
        assertTrue(tokenGroups.contains(TOKEN_5));
        assertEquals(5, tokenGroups.size());

        List<Object> groupesSid = attrs.get(destAttr);
        assertNotNull(groupesSid);
        assertTrue(groupesSid.contains(GROUP_1_SID));
        assertTrue(groupesSid.contains(GROUP_2_SID));
        assertTrue(groupesSid.contains(GROUP_3_SID));
        assertTrue(groupesSid.contains(GROUP_4_SID));
        assertTrue(groupesSid.contains(GROUP_5_SID));
        assertEquals(5, groupesSid.size());

    }

}
