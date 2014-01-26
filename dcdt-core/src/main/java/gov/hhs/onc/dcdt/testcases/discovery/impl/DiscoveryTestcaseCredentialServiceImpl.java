package gov.hhs.onc.dcdt.testcases.discovery.impl;

import gov.hhs.onc.dcdt.data.tx.services.impl.AbstractToolBeanService;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseCredential;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseCredentialDao;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseCredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Scope("singleton")
@Service("discoveryTestcaseCredServiceImpl")
public class DiscoveryTestcaseCredentialServiceImpl extends AbstractToolBeanService<DiscoveryTestcaseCredential, DiscoveryTestcaseCredentialDao> implements
    DiscoveryTestcaseCredentialService {
    @Autowired
    @Override
    protected void setBeanDao(DiscoveryTestcaseCredentialDao beanDao) {
        super.setBeanDao(beanDao);
    }
}
