package jaxb.xmlnsform.qualified;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Person", namespace = "http://a")
@XmlAccessorType(XmlAccessType.FIELD)
public class Person {
    @XmlElement
    private String firstNameWithPrefix;

    @XmlElement
    private String LastNameWithOutPrefix;

    public String getFirstNameWithPrefix() {
        return firstNameWithPrefix;
    }

    public void setFirstNameWithPrefix(String firstNameWithPrefix) {
        this.firstNameWithPrefix = firstNameWithPrefix;
    }

    public String getLastNameWithOutPrefix() {
        return LastNameWithOutPrefix;
    }

    public void setLastNameWithOutPrefix(String lastNameWithOutPrefix) {
        LastNameWithOutPrefix = lastNameWithOutPrefix;
    }

    @Override
    public String toString() {
        return "Person{nameWithPrefix='" + firstNameWithPrefix + "', nameWithOutPrefix=" + LastNameWithOutPrefix + '}';
    }
}