package net.archigny.cas.persondir.adutils;


import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class TokenGroupsSpringBeansTest {

    public final static Logger log = LoggerFactory.getLogger(TokenGroupsSpringBeansTest.class);
    
    public final static String UID = "test1";
    
    
    @Test
    public void SpringTest() {
        ApplicationContext app = new ClassPathXmlApplicationContext("app-test.xml");
        IPersonAttributeDao personDir = (IPersonAttributeDao) app.getBean("attributeRepository");
        IPersonAttributes attrs = personDir.getPerson(UID);
        log.info("Retrived " + attrs.getAttributes().toString());
        
        List<Object> groupes = attrs.getAttributeValues("groupes");
        
        log.info("Groupes modifiés : " + Arrays.toString(groupes.toArray()));
        assertTrue(groupes.contains("Utilisateurs"));
        assertTrue(groupes.contains("Utilisateurs du domaine"));
        assertEquals(2, groupes.size());
    }

}
