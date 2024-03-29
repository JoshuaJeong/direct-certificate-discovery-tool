package gov.hhs.onc.dcdt.config.instance.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import gov.hhs.onc.dcdt.beans.impl.AbstractToolNamedBean;
import gov.hhs.onc.dcdt.config.instance.InstanceMailAddressConfig;
import gov.hhs.onc.dcdt.mail.MailAddress;
import gov.hhs.onc.dcdt.mail.config.MailGatewayConfig;
import gov.hhs.onc.dcdt.mail.config.MailGatewayCredentialConfig;
import gov.hhs.onc.dcdt.net.utils.ToolInetAddressUtils;
import java.net.InetAddress;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("instanceMailAddrConfig")
public class InstanceMailAddressConfigImpl extends AbstractToolNamedBean implements InstanceMailAddressConfig {
    private MailGatewayConfig gatewayConfig;
    private MailGatewayCredentialConfig gatewayCredConfig;
    private MailAddress mailAddr;
    private boolean processed;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!this.mailAddr.hasPersonalPart()) {
            this.mailAddr.setPersonalPart(this.nameDisplay);
        }

        if (!this.mailAddr.hasLocalPart()) {
            this.mailAddr.setLocalPart(this.name);
        }

        if (this.mailAddr.hasDomainName()) {
            // noinspection ConstantConditions
            InetAddress gatewayHost = null;
            String gatewayHostName;

            // noinspection ConstantConditions
            if (!this.gatewayConfig.hasHost() || (gatewayHost = this.gatewayConfig.getHost()).isAnyLocalAddress()
                || StringUtils.isBlank((gatewayHostName = gatewayHost.getHostName()))
                || Objects.equals(gatewayHostName, InetAddress.getLocalHost().getHostName())) {
                String gatewayHostAddr = ((gatewayHost != null) ? gatewayHost.getHostAddress() : null), mailAddrDomainNamePart =
                    this.mailAddr.getDomainNamePart();

                // noinspection ConstantConditions
                this.gatewayConfig.setHost((ToolInetAddressUtils.isAddress(gatewayHostAddr) ? ToolInetAddressUtils.getByAddress(mailAddrDomainNamePart,
                    gatewayHostAddr) : ToolInetAddressUtils.getByName(mailAddrDomainNamePart)));
            }
        }

        if (!this.gatewayCredConfig.hasId()) {
            this.gatewayCredConfig.setId(this.mailAddr);
        }
    }

    @Override
    public MailGatewayConfig getGatewayConfig() {
        return this.gatewayConfig;
    }

    @Override
    public void setGatewayConfig(MailGatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public MailGatewayCredentialConfig getGatewayCredentialConfig() {
        return this.gatewayCredConfig;
    }

    @Override
    public void setGatewayCredentialConfig(MailGatewayCredentialConfig gatewayCredConfig) {
        this.gatewayCredConfig = gatewayCredConfig;
    }

    @Override
    public MailAddress getMailAddress() {
        return this.mailAddr;
    }

    @Override
    public void setMailAddress(MailAddress mailAddr) {
        this.mailAddr = mailAddr;
    }

    @Override
    public boolean isProcessed() {
        return this.processed;
    }

    @Override
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
