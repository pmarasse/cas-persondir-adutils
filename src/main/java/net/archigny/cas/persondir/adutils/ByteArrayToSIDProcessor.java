package net.archigny.cas.persondir.adutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.support.LdapUtils;

import net.archigny.cas.persondir.processors.IAttributesProcessor;

public class ByteArrayToSIDProcessor implements IAttributesProcessor, InitializingBean {

    private static final Logger log                   = LoggerFactory.getLogger(ByteArrayToSIDProcessor.class);

    /**
     * True if source attribute should be deleted after translation
     */
    private boolean             deleteSourceAttribute = true;

    /**
     * Source attribute name where byte[] of tokenGroups are found
     */
    private String              sourceAttribute;

    /**
     * Destination attribute name where
     */
    private String              targetAttribute;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (sourceAttribute == null) {
            throw new BeanCreationException("sourceAttribute cannot be null");
        }
        if (sourceAttribute.equalsIgnoreCase(targetAttribute)) {
            if (log.isDebugEnabled()) {
                log.debug("useless definition of targetAttribute = sourceAttribute");
            }
            targetAttribute = null;
        }

    }

    @Override
    public Set<String> getPossibleUserAttributeNames() {

        if (targetAttribute == null) {
            return null;
        }
        Set<String> attributeNames = new HashSet<String>();
        attributeNames.add(targetAttribute);

        return attributeNames;
    }

    @Override
    public void processAttributes(final Map<String, List<Object>> attrs) {

        if (!attrs.containsKey(sourceAttribute)) {
            return;
        }

        final List<Object> tokens = attrs.get(sourceAttribute);
        final List<Object> sids = new ArrayList<Object>(tokens.size());

        for (Object token : tokens) {
            try {
                final String sid = LdapUtils.convertBinarySidToString((byte[]) token);
                if (sid != null) {
                    sids.add(sid);
                }
            } catch (ClassCastException e) {
                // Silently ignore...
            }
        }

        if (targetAttribute == null) {
            // Replacing source attribute
            attrs.put(sourceAttribute, sids);
        } else {
            // Adding a new attribute
            attrs.put(targetAttribute, sids);

            if (deleteSourceAttribute) {
                // And remove old one...
                attrs.remove(sourceAttribute);
            }
        }

    }

    public boolean isDeleteSourceAttribute() {

        return deleteSourceAttribute;
    }

    public void setDeleteSourceAttribute(final boolean deleteSourceAttribute) {

        this.deleteSourceAttribute = deleteSourceAttribute;
    }

    public String getSourceAttribute() {

        return sourceAttribute;
    }

    public void setSourceAttribute(final String sourceAttribute) {

        this.sourceAttribute = sourceAttribute;
    }

    public String getTargetAttribute() {

        return targetAttribute;
    }

    public void setTargetAttribute(final String targetAttribute) {

        this.targetAttribute = targetAttribute;
    }

}
