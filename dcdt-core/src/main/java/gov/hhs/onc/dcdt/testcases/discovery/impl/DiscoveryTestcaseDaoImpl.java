package gov.hhs.onc.dcdt.testcases.discovery.impl;

import gov.hhs.onc.dcdt.data.dao.impl.AbstractToolBeanDao;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseDao;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

@DependsOn({ "instanceConfigDataSourceInit" })
@Repository("discoveryTestcaseDaoImpl")
public class DiscoveryTestcaseDaoImpl extends AbstractToolBeanDao<DiscoveryTestcase> implements DiscoveryTestcaseDao {
    public DiscoveryTestcaseDaoImpl() {
        super(DiscoveryTestcase.class, DiscoveryTestcaseImpl.class);
    }
}
