package net.archigny.cas.persondir.adutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;

import net.archigny.cas.persondir.processors.IAttributesProcessor;
import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;

public class TokenGroupsToAttributeProcessor implements IAttributesProcessor, InitializingBean {

    private final Logger                log                         = LoggerFactory
                                                                            .getLogger(TokenGroupsToAttributeProcessor.class);

    /**
     * Attribute to retrieve from AD with global catalog enabled. According to LDAPv3, ;binary is appended to retrieve data as a
     * byte[] instead of a String
     * 
     * @see http://msdn.microsoft.com/en-us/library/cc223395%28v=prot.13%29
     * @see http://docs.oracle.com/javase/jndi/tutorial/ldap/misc/attrs.html
     */
    private static String[]             TOKEN_GROUPS_ATTRS          = { "tokenGroups;binary" };

    /**
     * Attribute name as send by spring-ldap (note that ;binary still exists in attribute name !)
     */
    private static String               TOKEN_GROUPS_ATTR           = "tokenGroups;binary";

    /**
     * Attribute to retrieve from AD with or without global catalog enabled. According to LDAPv3, ;binary is appended to retrieve
     * data as a byte[] instead of a String
     * 
     * @see http://msdn.microsoft.com/en-us/library/cc223395%28v=prot.13%29
     * @see http://docs.oracle.com/javase/jndi/tutorial/ldap/misc/attrs.html
     */
    private static String[]             TOKEN_GROUPS_NO_GC_ATTRS    = { "tokenGroupsNoGCAcceptable;binary" };

    /**
     * Attribute name as send by spring-ldap (note that ;binary still exists in attribute name !)
     */
    private static String               TOKEN_GROUPS_NO_GC_ATTR     = "tokenGroupsNoGCAcceptable;binary";

    /**
     * tokenGroups registry used to translate SID to ldap groups
     */
    IActiveDirectoryTokenGroupsRegistry tokenRegistry;

    /**
     * source attribute containing group tokens in byte[] or String form
     */
    private String                      dnAttribute;

    /**
     * target attributes containing decoded groups
     */
    private String                      targetAtribute;

    /**
     * Specify if an IllegalFormatException will be raised if input data of UUID is invalid (eg not 128bit long)
     */
    private boolean                     raiseIllegalFormatException = false;

    /**
     * True : tokenGroups attribute should be used, False : tokenGroupsNoGCAcceptable is used
     */
    private boolean                     useTokenGroupsAttribute     = true;

    /**
     * LDAP Template used to retrieve tokenGroups
     */
    private LdapTemplate                ldapTemplate;

    /**
     * LDAP Context Source
     */
    private ContextSource               contextSource;

    /**
     * Base DN used by Ldap Context Source (cannot fetch it from "ContextSource"...)
     */
    private LdapName                    contextSourceBaseDN;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (contextSource == null) {
            throw new BeanCreationException("contextSrouce cannot be null");
        }
        if (tokenRegistry == null) {
            throw new BeanCreationException("tokenRegistry cannot be null");
        }
        if (dnAttribute == null) {
            throw new BeanCreationException("dnAttribute cannot be null");
        }
        if (targetAtribute == null) {
            throw new BeanCreationException("targetAttribute cannot be null");
        }

        ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setIgnoreNameNotFoundException(true);

    }

    @Override
    public void processAttributes(Map<String, List<Object>> attributes) {

        List<Object> dnValues = attributes.get(dnAttribute);

        // Source attribute found ?
        if ((dnValues == null) || (dnValues.isEmpty())) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("DN value found : [" + dnValues.get(0) + "] processing...");
        }

        try {
            Name userDN = new LdapName((String) dnValues.get(0));

            if (contextSourceBaseDN != null) {
                if (userDN.startsWith(contextSourceBaseDN)) {

                    // Get rid of base DN before querying the directory
                    userDN = userDN.getSuffix(contextSourceBaseDN.size());
                    if (log.isDebugEnabled()) {
                        log.debug("base DN of contextSource found as suffix of userDN. deleting, new userDN : " + userDN.toString());
                    }
                }
            }

            @SuppressWarnings("unchecked")
            List<Object> targetValues = (List<Object>) ldapTemplate.lookup(userDN, (useTokenGroupsAttribute ? TOKEN_GROUPS_ATTRS
                    : TOKEN_GROUPS_NO_GC_ATTRS), new TokenGroupMapper());

            if ((targetValues != null) && (!targetValues.isEmpty())) {
                attributes.put(targetAtribute, targetValues);
            } else {
                log.info("This DN seems to have no tokenGroups");
            }

        } catch (InvalidNameException e1) {
            log.error("Unable to build LdapName from " + dnValues.get(0));
        }

    }

    @Override
    public Set<String> getPossibleUserAttributeNames() {

        Set<String> attributeNames = new HashSet<String>();
        attributeNames.add(targetAtribute);

        return attributeNames;
    }

    // Getters et setters

    public IActiveDirectoryTokenGroupsRegistry getTokenRegistry() {

        return tokenRegistry;
    }

    public void setTokenRegistry(IActiveDirectoryTokenGroupsRegistry tokenRegistry) {

        this.tokenRegistry = tokenRegistry;
    }

    public String getDnAttribute() {

        return dnAttribute;
    }

    public void setDnAttribute(String dnAttribute) {

        this.dnAttribute = dnAttribute;
    }

    public String getTargetAtribute() {

        return targetAtribute;
    }

    public void setTargetAtribute(String targetAtribute) {

        this.targetAtribute = targetAtribute;
    }

    public boolean isRaiseIllegalFormatException() {

        return raiseIllegalFormatException;
    }

    public void setRaiseIllegalFormatException(boolean raiseIllegalFormatException) {

        this.raiseIllegalFormatException = raiseIllegalFormatException;
    }

    public ContextSource getContextSource() {

        return contextSource;
    }

    public void setContextSource(ContextSource contextSource) {

        this.contextSource = contextSource;
    }

    
    public boolean isUseTokenGroupsAttribute() {
    
        return useTokenGroupsAttribute;
    }

    
    public void setUseTokenGroupsAttribute(boolean useTokenGroupsAttribute) {
    
        this.useTokenGroupsAttribute = useTokenGroupsAttribute;
    }

    // Wrapper getter around LdapName
    public String getBaseDN() {

        return (contextSourceBaseDN == null ? "" : contextSourceBaseDN.toString());
    }

    public void setBaseDN(String baseDN) {

        if (baseDN == null) {
            throw new IllegalArgumentException("baseDN cannot be null");
        }
        try {
            this.contextSourceBaseDN = new LdapName(baseDN);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException(e);
        }

    }

    /**
     * Private class which maps tokenGroups to real groups.
     * 
     * @author Philippe Marasse <philippe.marasse@laposte.net> *
     */
    private class TokenGroupMapper implements ContextMapper {

        List<Object> decodedValues = new ArrayList<Object>();

        @Override
        public Object mapFromContext(Object ctx) {

            DirContextAdapter context = (DirContextAdapter) ctx;
            if (log.isDebugEnabled()) {
                log.debug("Attributes returned by context : " + context.getAttributes().toString());
            }

            Object[] values = context.getObjectAttributes(useTokenGroupsAttribute ? TOKEN_GROUPS_ATTR : TOKEN_GROUPS_NO_GC_ATTR);

            if (values != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Attribute " + (useTokenGroupsAttribute ? TOKEN_GROUPS_ATTR : TOKEN_GROUPS_NO_GC_ATTR) + " Values : "
                            + Arrays.toString(values));
                }

                for (Object rawValue : values) {
                    String group = tokenRegistry.getDnFromToken((byte[]) rawValue);
                    if (group != null) {
                        decodedValues.add(group);
                    } else {
                        if (log.isInfoEnabled()) {
                            log.info("unable to decode raw tokenGroup class : " + rawValue.getClass().getCanonicalName());
                        }
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Returning decoded groups : " + Arrays.toString(decodedValues.toArray()));
            }
            return decodedValues;
        }

    }

}
