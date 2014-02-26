package gov.hhs.onc.dcdt.testcases.results;

import gov.hhs.onc.dcdt.beans.ToolBean;
import gov.hhs.onc.dcdt.mail.MailAddress;
import java.util.List;

public interface CertificateDiscoveryService extends ToolBean {
    public List<ToolTestcaseResultStep> runCertificateDiscoverySteps(MailAddress directAddr, ToolTestcaseResultHolder resultHolder,
        List<ToolTestcaseResultStep> certDiscoverySteps) throws ToolTestcaseResultException;
}