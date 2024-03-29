package gov.hhs.onc.dcdt.testcases.discovery.results.sender;

import gov.hhs.onc.dcdt.mail.MailAddress;
import gov.hhs.onc.dcdt.mail.sender.ToolMailSenderService;
import gov.hhs.onc.dcdt.testcases.discovery.results.DiscoveryTestcaseResult;

public interface DiscoveryTestcaseResultSenderService extends ToolMailSenderService {
    public void send(DiscoveryTestcaseResult discoveryTestcaseResult, MailAddress to) throws Exception;
}
