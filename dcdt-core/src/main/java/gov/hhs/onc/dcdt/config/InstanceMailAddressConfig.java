package gov.hhs.onc.dcdt.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import gov.hhs.onc.dcdt.beans.ToolNamedBean;
import gov.hhs.onc.dcdt.config.impl.InstanceMailAddressConfigImpl;
import gov.hhs.onc.dcdt.mail.MailAddress;

@JsonSubTypes({ @Type(InstanceMailAddressConfigImpl.class) })
public interface InstanceMailAddressConfig extends ToolNamedBean {
    @JsonProperty("mailAddr")
    public MailAddress getMailAddress();

    public void setMailAddress(MailAddress mailAddr);
}