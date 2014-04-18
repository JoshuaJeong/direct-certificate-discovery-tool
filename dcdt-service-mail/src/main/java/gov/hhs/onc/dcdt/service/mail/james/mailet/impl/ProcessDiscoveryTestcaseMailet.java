package gov.hhs.onc.dcdt.service.mail.james.mailet.impl;

import gov.hhs.onc.dcdt.beans.utils.ToolBeanFactoryUtils;
import gov.hhs.onc.dcdt.mail.MailAddress;
import gov.hhs.onc.dcdt.mail.impl.ToolMimeMessageHelper;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase.DiscoveryTestcaseMailAddressPredicate;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseProcessor;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseSubmission;
import gov.hhs.onc.dcdt.testcases.discovery.mail.DiscoveryTestcaseMailMapping;
import gov.hhs.onc.dcdt.testcases.discovery.mail.DiscoveryTestcaseMailMapping.DiscoveryTestcaseMailMappingPredicate;
import gov.hhs.onc.dcdt.testcases.discovery.mail.DiscoveryTestcaseMailMappingService;
import gov.hhs.onc.dcdt.testcases.discovery.results.DiscoveryTestcaseResult;
import gov.hhs.onc.dcdt.testcases.discovery.results.sender.DiscoveryTestcaseResultSenderService;
import gov.hhs.onc.dcdt.utils.ToolClassUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDiscoveryTestcaseMailet extends AbstractToolMailet {
    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessDiscoveryTestcaseMailet.class);

    @Override
    protected void serviceInternal(ToolMimeMessageHelper mimeMsgHelper) throws Exception {
        DiscoveryTestcaseProcessor discoveryTestcaseProc = ToolBeanFactoryUtils.getBeanOfType(this.appContext, DiscoveryTestcaseProcessor.class);
        MailAddress to = mimeMsgHelper.getTo(), from = mimeMsgHelper.getFrom();
        DiscoveryTestcase discoveryTestcase =
            CollectionUtils.find(ToolBeanFactoryUtils.getBeansOfType(this.appContext, DiscoveryTestcase.class), new DiscoveryTestcaseMailAddressPredicate(to));
        DiscoveryTestcaseSubmission discoveryTestcaseSubmission =
            ToolBeanFactoryUtils.createBeanOfType(this.appContext, DiscoveryTestcaseSubmission.class, discoveryTestcase, mimeMsgHelper);
        // noinspection ConstantConditions
        DiscoveryTestcaseResult discoveryTestcaseResult = discoveryTestcaseProc.process(discoveryTestcaseSubmission);

        // noinspection ConstantConditions
        DiscoveryTestcaseMailMapping mailMapping =
            CollectionUtils.find(ToolBeanFactoryUtils.getBeanOfType(this.appContext, DiscoveryTestcaseMailMappingService.class).getBeans(),
                new DiscoveryTestcaseMailMappingPredicate(from));

        if (mailMapping != null) {
            sendResultMail(to, discoveryTestcase, discoveryTestcaseResult, mailMapping);
        } else {
            LOGGER.error(String.format(
                "Unable to find mail address for sending results associated with Discovery testcase (name=%s, mailAddr=%s) mail (from=%s).",
                discoveryTestcase.getName(), to, from));
        }
    }

    private void sendResultMail(MailAddress to, DiscoveryTestcase discoveryTestcase, DiscoveryTestcaseResult discoveryTestcaseResult,
        DiscoveryTestcaseMailMapping mailMapping) throws Exception {
        DiscoveryTestcaseResultSenderService discoveryTestcaseResultSenderService =
            ToolBeanFactoryUtils.getBeanOfType(this.appContext, DiscoveryTestcaseResultSenderService.class);
        MailAddress resultsAddr = mailMapping.getResultsAddress();
        // noinspection ConstantConditions
        discoveryTestcaseResultSenderService.send(discoveryTestcaseResult, resultsAddr);

        LOGGER.info(String.format("Sent Discovery testcase result mail (to=%s) for Discovery testcase (name=%s, mailAddr=%s) from sender service (class=%s).",
            resultsAddr, discoveryTestcase.getName(), to, ToolClassUtils.getName(DiscoveryTestcaseResultSenderService.class)));
    }
}
