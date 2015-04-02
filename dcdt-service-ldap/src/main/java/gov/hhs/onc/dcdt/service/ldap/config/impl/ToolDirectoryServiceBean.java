package gov.hhs.onc.dcdt.service.ldap.config.impl;

import gov.hhs.onc.dcdt.beans.utils.ToolBeanFactoryUtils;
import gov.hhs.onc.dcdt.collections.ToolTransformer;
import gov.hhs.onc.dcdt.config.instance.InstanceLdapConfig;
import gov.hhs.onc.dcdt.crypto.DataEncoding;
import gov.hhs.onc.dcdt.crypto.credentials.CredentialInfo;
import gov.hhs.onc.dcdt.crypto.utils.CertificateUtils;
import gov.hhs.onc.dcdt.ldap.ToolCoreSchemaConstants;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase;
import gov.hhs.onc.dcdt.testcases.discovery.credentials.DiscoveryTestcaseCredential;
import gov.hhs.onc.dcdt.testcases.discovery.credentials.DiscoveryTestcaseCredentialLocation;
import gov.hhs.onc.dcdt.utils.ToolIteratorUtils;
import gov.hhs.onc.dcdt.utils.ToolStreamUtils;
import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.LdapServerBean;
import org.apache.directory.server.config.beans.TcpTransportBean;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;

public class ToolDirectoryServiceBean extends DirectoryServiceBean implements ApplicationContextAware, InitializingBean {
    private static Entry transformDiscoveryTestcaseCredentialEntry(DiscoveryTestcaseCredential discoveryTestcaseCred, SchemaManager schemaManager, Dn
        dataPartitionSuffix) throws Exception {
        String discoveryTestcaseCredName = discoveryTestcaseCred.getName();

        Entry discoveryTestcaseCredEntry =
            new DefaultEntry(schemaManager, dataPartitionSuffix.add(new Dn(new Rdn(SchemaConstants.CN_AT, discoveryTestcaseCredName))));
        discoveryTestcaseCredEntry.add(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.INET_ORG_PERSON_OC, SchemaConstants.ORGANIZATIONAL_PERSON_OC,
            SchemaConstants.PERSON_OC, SchemaConstants.TOP_OC);
        discoveryTestcaseCredEntry.add(SchemaConstants.CN_AT, discoveryTestcaseCredName);
        // noinspection ConstantConditions
        discoveryTestcaseCredEntry.add(ToolCoreSchemaConstants.ATTR_TYPE_NAME_MAIL, discoveryTestcaseCred.getLocation().getMailAddress().toAddress());
        discoveryTestcaseCredEntry.add(SchemaConstants.SN_AT, discoveryTestcaseCredName);
        // noinspection ConstantConditions
        discoveryTestcaseCredEntry.add(ToolCoreSchemaConstants.ATTR_TYPE_NAME_USER_CERT,
            CertificateUtils.writeCertificate(discoveryTestcaseCred.getCredentialInfo().getCertificateDescriptor().getCertificate(), DataEncoding.DER));

        return discoveryTestcaseCredEntry;
    }

    private static boolean hasDiscoveryTestcaseCredentialEntry(DiscoveryTestcaseCredential discoveryTestcaseCred, String dataPartitionId) {
        DiscoveryTestcaseCredentialLocation discoveryTestcaseCredLoc;
        CredentialInfo discoveryTestcaseCredInfo;

        // noinspection ConstantConditions
        return discoveryTestcaseCred.hasBindingType() && discoveryTestcaseCred.getBindingType().isBound() && discoveryTestcaseCred.hasLocation()
            && (discoveryTestcaseCredLoc = discoveryTestcaseCred.getLocation()).getType().isLdap() && discoveryTestcaseCredLoc.hasLdapConfig()
            && discoveryTestcaseCredLoc.getLdapConfig().getDataPartitionId().equals(dataPartitionId) && discoveryTestcaseCredLoc.hasMailAddress()
            && discoveryTestcaseCred.hasCredentialInfo()
            && (discoveryTestcaseCredInfo = discoveryTestcaseCred.getCredentialInfo()).hasCertificateDescriptor()
            && discoveryTestcaseCredInfo.getCertificateDescriptor().hasCertificate();
    }

    private AbstractApplicationContext appContext;
    private CacheService cacheService;
    private Collection<Entry> dataEntries;
    private InstanceLayout instanceLayout;
    private InstanceLdapConfig ldapConfig;
    private SchemaManager schemaManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        LdapServerBean ldapServerBean = new LdapServerBean();
        ldapServerBean.setSearchBaseDn(new Dn(ServerDNConstants.USERS_SYSTEM_DN));
        ldapServerBean.setServerId(this.ldapConfig.getServerId());

        TcpTransportBean tcpTransportBean = new TcpTransportBean();
        tcpTransportBean.setSystemPort(this.ldapConfig.getPort());
        // noinspection ConstantConditions
        tcpTransportBean.setTransportAddress(this.ldapConfig.getHost().getHostAddress());
        ldapServerBean.addTransports(tcpTransportBean);

        this.addServers(ldapServerBean);

        String dataPartitionId = this.ldapConfig.getDataPartitionId();
        Dn dataPartitionSuffix = this.ldapConfig.getDataPartitionSuffix();

        AvlPartitionBean dataPartitionBean = new AvlPartitionBean();
        dataPartitionBean.setPartitionId(dataPartitionId);
        dataPartitionBean.setPartitionSuffix(dataPartitionSuffix);
        this.addPartitions(dataPartitionBean);

        this.dataEntries =
            ToolStreamUtils.transform(ToolStreamUtils.filter(
                IteratorUtils.asIterable(ToolIteratorUtils.chainedIterator(ToolStreamUtils.transform(
                    ToolBeanFactoryUtils.getBeansOfType(this.appContext, DiscoveryTestcase.class), DiscoveryTestcase::extractCredentials))),
                cred -> ToolDirectoryServiceBean.hasDiscoveryTestcaseCredentialEntry(cred, dataPartitionId)), ToolTransformer.wrap(cred ->
                ToolDirectoryServiceBean.transformDiscoveryTestcaseCredentialEntry(cred, this.schemaManager, dataPartitionSuffix)));
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.appContext = (AbstractApplicationContext) appContext;
    }

    @Nullable
    public CacheService getCacheService() {
        return this.cacheService;
    }

    public void setCacheService(@Nullable CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public boolean hasDataEntries() {
        return !CollectionUtils.isEmpty(this.dataEntries);
    }

    @Nullable
    public Collection<Entry> getDataEntries() {
        return this.dataEntries;
    }

    public InstanceLayout getInstanceLayout() {
        return this.instanceLayout;
    }

    public void setInstanceLayout(InstanceLayout instanceLayout) {
        this.instanceLayout = instanceLayout;
    }

    public InstanceLdapConfig getLdapConfig() {
        return this.ldapConfig;
    }

    public void setLdapConfig(InstanceLdapConfig ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public SchemaManager getSchemaManager() {
        return this.schemaManager;
    }

    public void setSchemaManager(SchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }
}
