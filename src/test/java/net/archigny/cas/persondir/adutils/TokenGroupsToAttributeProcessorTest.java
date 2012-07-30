package net.archigny.cas.persondir.adutils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.archigny.cas.persondir.ldap.LdapPersonAttributeDao;
import net.archigny.cas.persondir.processors.IAttributesProcessor;
import net.archigny.utils.ad.impl.CachingADTokenGroupsRegistry;

import org.jasig.services.persondir.IPersonAttributes;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.LdapContextSource;

public class TokenGroupsToAttributeProcessorTest {

    public final static Logger            log          = LoggerFactory.getLogger(TokenGroupsToAttributeProcessorTest.class);

    public final static String            BASE_DN      = "dc=in,dc=archigny,dc=org";

    public final static String            BIND_DN      = "cn=Application Test,ou=Applications,ou=Utilisateurs,dc=in,dc=archigny,dc=org";

    public final static String            BIND_PW      = "123456";

    public final static String            SERVER_URL   = "ldap://win2k8.in.archigny.org";

    public final static String            LDAP_FILTER  = "(samAccountName={0})";

    public final static String            TOKEN_GROUPS = "tokenGroups";

    public final static String            CN           = "cn";

    public final static String            DN_ATTR      = "userDn";

    public final static String            TARGET_NAME  = "groups";

    public final static String            USER1_ID     = "test1";

    public final static String            USER2_ID     = "philippe";

    private LdapPersonAttributeDao personAttributeDao;

    private LdapContextSource      ldapCS;
    
    private TokenGroupsToAttributeProcessor processor;

    @Before
    public void globalSetUp() throws Exception {

/*        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.ldap.attributes.binary","tokenGroups tokenGroupsNoGCAcceptable");
  */      
        // Crée une contextSource LDAP bean
        log.debug("Creating LdapContextSource");
        ldapCS = new LdapContextSource();
        ldapCS.setBase(BASE_DN);
        ldapCS.setPooled(true);
        ldapCS.setUserDn(BIND_DN);
        ldapCS.setPassword(BIND_PW);
        ldapCS.setUrl(SERVER_URL);       
        //ldapCS.setBaseEnvironmentProperties(env);
        ldapCS.afterPropertiesSet();
        log.debug("LdapContextSource created");

        // Initialize LdapPersonAttributeDao bean
        log.debug("preparing LdapPersonAttributeDao");

        // List of queried Attributes
        List<String> queriedAttributes = new ArrayList<String>();
        // queriedAttributes.add(TOKEN_GROUPS);
        queriedAttributes.add(CN);

        // token registry bean
        log.debug("Creating CachingADTokenGroupsRegistry");
        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setContextSource(ldapCS);
        tokenRegistry.setMaxElements(20);
        tokenRegistry.setContextSourceBaseDN(BASE_DN);
        tokenRegistry.afterPropertiesSet();
        log.debug("CachingADTokenGroupsRegistry created");

        // List of processors
        List<IAttributesProcessor> processors = new ArrayList<IAttributesProcessor>();

        // TokengroupProcessor bean
        log.debug("Creating TokenGroupsToAttributeProcessor");
        processor = new TokenGroupsToAttributeProcessor();
        processor.setTokenRegistry(tokenRegistry);
        processor.setDnAttribute(DN_ATTR);
        processor.setTargetAtribute(TARGET_NAME);
        processor.setContextSource(ldapCS);
        processor.afterPropertiesSet();
        log.debug("TokenGroupsToAttributeProcessor created");

        processors.add(processor);

        log.debug("Creating LdapPersonAttributeDao");
        personAttributeDao = new LdapPersonAttributeDao();
        personAttributeDao.setContextSource(ldapCS);
        personAttributeDao.setQueriedAttributes(queriedAttributes);
        personAttributeDao.setProcessors(processors);
        personAttributeDao.setLdapFilter(LDAP_FILTER);
        personAttributeDao.setDnAttributeName(DN_ATTR);
        personAttributeDao.afterPropertiesSet();
        log.debug("LdapPersonAttributeDao created");

        log.debug("Context fully initialized");
    }

    @Test
    public void RealTest() {

        log.debug("RealTest");

        IPersonAttributes attributes = personAttributeDao.getPerson(USER1_ID);
        List<Object> values = attributes.getAttributeValues(TARGET_NAME);
        log.debug("retrieved target attributes : " + Arrays.toString(values.toArray()));
        
        // User1 member of cn=Utilisateurs,cn=Builtin,dc=in,dc=archigny,dc=org
        // and cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org]

        log.info("2 groups expected for user test1");
        assertEquals(2, values.size());

        HashSet<DistinguishedName> currentGroups = new HashSet<DistinguishedName>(2);
        
        for (Object object : values) {
            currentGroups.add(new DistinguishedName((String) object));
        }
        
        DistinguishedName expectedGroup1 = new DistinguishedName("cn=Utilisateurs,cn=Builtin,dc=in,dc=archigny,dc=org");
        DistinguishedName expectedGroup2 = new DistinguishedName("cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org");
        
        assertTrue(currentGroups.contains(expectedGroup1));
        assertTrue(currentGroups.contains(expectedGroup2));
        
        attributes = personAttributeDao.getPerson(USER2_ID);
        values = attributes.getAttributeValues(TARGET_NAME);
        log.debug("retrieved target attributes : " + Arrays.toString(values.toArray()));
        
        /* Groups expected : 
         * cn=Utilisateurs,cn=Builtin,dc=in,dc=archigny,dc=org, 
         * cn=Groupe local indirect,ou=Groupes,dc=in,dc=archigny,dc=org, 
         * cn=groupe indirect,ou=Groupes,dc=in,dc=archigny,dc=org, 
         * cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org, 
         * cn=groupe direct,ou=Groupes,dc=in,dc=archigny,dc=org]
         */

        assertEquals(5, values.size());
        currentGroups.clear();
        for (Object object : values) {
            currentGroups.add(new DistinguishedName((String) object));
        }

        DistinguishedName expectedGroup3 = new DistinguishedName("cn=Groupe local indirect,ou=Groupes,dc=in,dc=archigny,dc=org");
        DistinguishedName expectedGroup4 = new DistinguishedName("cn=groupe indirect,ou=Groupes,dc=in,dc=archigny,dc=org");
        DistinguishedName expectedGroup5 = new DistinguishedName("cn=groupe direct,ou=Groupes,dc=in,dc=archigny,dc=org");
        
        assertTrue(currentGroups.contains(expectedGroup1));
        assertTrue(currentGroups.contains(expectedGroup2));
        assertTrue(currentGroups.contains(expectedGroup3));
        assertTrue(currentGroups.contains(expectedGroup4));
        assertTrue(currentGroups.contains(expectedGroup5));
        
        log.info("RealTest end.");
    }

    @Test
    public void RealTestNoGC() {

        log.debug("RealTestNoGC : same test but using tokenGroupsNoGCAcceptable instead of tokenGroups attribute");
        processor.setUseTokenGroupsAttribute(false);
        
        IPersonAttributes attributes = personAttributeDao.getPerson(USER1_ID);
        List<Object> values = attributes.getAttributeValues(TARGET_NAME);
        log.debug("retrieved target attributes : " + Arrays.toString(values.toArray()));
        
        // User1 member of cn=Utilisateurs,cn=Builtin,dc=in,dc=archigny,dc=org
        // and cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org]

        log.info("2 groups expected for user test1");
        assertEquals(2, values.size());

        HashSet<DistinguishedName> currentGroups = new HashSet<DistinguishedName>(2);
        
        for (Object object : values) {
            currentGroups.add(new DistinguishedName((String) object));
        }
        
        DistinguishedName expectedGroup1 = new DistinguishedName("cn=Utilisateurs,cn=Builtin,dc=in,dc=archigny,dc=org");
        DistinguishedName expectedGroup2 = new DistinguishedName("cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org");
        
        assertTrue(currentGroups.contains(expectedGroup1));
        assertTrue(currentGroups.contains(expectedGroup2));
        
        attributes = personAttributeDao.getPerson(USER2_ID);
        values = attributes.getAttributeValues(TARGET_NAME);
        log.debug("retrieved target attributes : " + Arrays.toString(values.toArray()));
        
        /* Groups expected : 
         * cn=Utilisateurs,cn=Builtin,dc=in,dc=archigny,dc=org, 
         * cn=Groupe local indirect,ou=Groupes,dc=in,dc=archigny,dc=org, 
         * cn=groupe indirect,ou=Groupes,dc=in,dc=archigny,dc=org, 
         * cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org, 
         * cn=groupe direct,ou=Groupes,dc=in,dc=archigny,dc=org]
         */

        assertEquals(5, values.size());
        currentGroups.clear();
        for (Object object : values) {
            currentGroups.add(new DistinguishedName((String) object));
        }

        DistinguishedName expectedGroup3 = new DistinguishedName("cn=Groupe local indirect,ou=Groupes,dc=in,dc=archigny,dc=org");
        DistinguishedName expectedGroup4 = new DistinguishedName("cn=groupe indirect,ou=Groupes,dc=in,dc=archigny,dc=org");
        DistinguishedName expectedGroup5 = new DistinguishedName("cn=groupe direct,ou=Groupes,dc=in,dc=archigny,dc=org");
        
        assertTrue(currentGroups.contains(expectedGroup1));
        assertTrue(currentGroups.contains(expectedGroup2));
        assertTrue(currentGroups.contains(expectedGroup3));
        assertTrue(currentGroups.contains(expectedGroup4));
        assertTrue(currentGroups.contains(expectedGroup5));
        
        
    }

    
}
