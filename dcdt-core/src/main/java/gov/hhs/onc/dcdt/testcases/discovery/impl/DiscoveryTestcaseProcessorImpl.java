package gov.hhs.onc.dcdt.testcases.discovery.impl;

import gov.hhs.onc.dcdt.beans.utils.ToolBeanFactoryUtils;
import gov.hhs.onc.dcdt.crypto.certs.CertificateInfo;
import gov.hhs.onc.dcdt.crypto.certs.CertificateName;
import gov.hhs.onc.dcdt.crypto.certs.CertificateValidator;
import gov.hhs.onc.dcdt.crypto.certs.impl.CertificateNameImpl;
import gov.hhs.onc.dcdt.crypto.credentials.CredentialInfo;
import gov.hhs.onc.dcdt.crypto.keys.KeyInfo;
import gov.hhs.onc.dcdt.mail.MailAddress;
import gov.hhs.onc.dcdt.mail.MailInfo;
import gov.hhs.onc.dcdt.mail.crypto.ToolSmimeException;
import gov.hhs.onc.dcdt.mail.crypto.utils.ToolSmimeUtils;
import gov.hhs.onc.dcdt.mail.impl.ToolMimeMessageHelper;
import gov.hhs.onc.dcdt.mail.utils.ToolMimePartUtils;
import gov.hhs.onc.dcdt.testcases.CertificateDiscoveryService;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseConfig;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseDescription;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseProcessor;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseSubmission;
import gov.hhs.onc.dcdt.testcases.discovery.credentials.DiscoveryTestcaseCredential;
import gov.hhs.onc.dcdt.testcases.discovery.credentials.DiscoveryTestcaseCredential.DiscoveryTestcaseCredentialValidPredicate;
import gov.hhs.onc.dcdt.testcases.discovery.results.DiscoveryTestcaseResult;
import gov.hhs.onc.dcdt.testcases.impl.AbstractToolTestcaseProcessor;
import gov.hhs.onc.dcdt.testcases.results.ToolTestcaseCertificateResultType;
import gov.hhs.onc.dcdt.testcases.results.ToolTestcaseResultException;
import gov.hhs.onc.dcdt.testcases.steps.ToolTestcaseCertificateStep;
import gov.hhs.onc.dcdt.testcases.steps.ToolTestcaseStep;
import gov.hhs.onc.dcdt.testcases.utils.ToolTestcaseCertificateUtils;
import gov.hhs.onc.dcdt.utils.ToolClassUtils;
import gov.hhs.onc.dcdt.utils.ToolListUtils;
import gov.hhs.onc.dcdt.utils.ToolStringUtils.ToolStrBuilder;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("discoveryTestcaseProcessorImpl")
public class DiscoveryTestcaseProcessorImpl
    extends
    AbstractToolTestcaseProcessor<DiscoveryTestcaseDescription, DiscoveryTestcaseConfig, DiscoveryTestcaseResult, DiscoveryTestcase, DiscoveryTestcaseSubmission>
    implements DiscoveryTestcaseProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(DiscoveryTestcaseProcessorImpl.class);

    @Autowired
    private CertificateDiscoveryService certDiscoveryService;

    @Autowired
    private Set<CertificateValidator> certValidators;

    @Override
    public DiscoveryTestcaseResult process(ToolMimeMessageHelper msgHelper, @Nullable DiscoveryTestcase discoveryTestcase) {
        DiscoveryTestcaseResult result = runDecryptionSteps(msgHelper, discoveryTestcase);
        result.setSuccessful(result.hasTestcase() && (result.getCredentialFound() == result.getCredentialExpected()));

        return result;
    }

    private DiscoveryTestcaseResult runDecryptionSteps(ToolMimeMessageHelper msgHelper, @Nullable DiscoveryTestcase discoveryTestcase) {
        MailInfo mailInfo = ToolBeanFactoryUtils.createBeanOfType(this.appContext, MailInfo.class);

        DiscoveryTestcaseResult result = ToolBeanFactoryUtils.createBeanOfType(this.appContext, DiscoveryTestcaseResult.class);
        // noinspection ConstantConditions
        result.setMailInfo(mailInfo);

        ToolStrBuilder decryptionErrorBuilder = new ToolStrBuilder();

        try {
            String msgId = msgHelper.getMimeMessage().getMessageID();
            MailAddress msgFrom = msgHelper.getFrom(), msgTo = msgHelper.getTo();

            if (discoveryTestcase == null) {
                throw new ToolSmimeException(String.format("Unable to find a matching Discovery testcase for mail MIME message (id=%s, from=%s, to=%s).",
                    msgId, msgFrom, msgTo));
            }

            SMIMEEnveloped enveloped = ToolSmimeUtils.getEnveloped(msgHelper);

            // noinspection ConstantConditions
            mailInfo.setEncryptedMessageHelper(msgHelper);

            result.setTestcase(discoveryTestcase);

            MimeBodyPart decryptedBodyPart = null;

            if (discoveryTestcase.hasCredentials()) {
                if (discoveryTestcase.hasTargetCredentials()) {
                    result.setCredentialExpected(CollectionUtils.find(discoveryTestcase.getTargetCredentials(),
                        DiscoveryTestcaseCredentialValidPredicate.INSTANCE));
                }

                CredentialInfo discoveryTestcaseCredInfo;
                KeyInfo discoveryTestcaseCredKeyInfo;
                CertificateInfo discoveryTestcaseCredCertInfo;

                // noinspection ConstantConditions
                for (DiscoveryTestcaseCredential discoveryTestcaseCred : discoveryTestcase.getCredentials()) {
                    // noinspection ConstantConditions
                    if (!discoveryTestcaseCred.hasCredentialInfo()
                        || !(discoveryTestcaseCredInfo = discoveryTestcaseCred.getCredentialInfo()).hasKeyDescriptor()
                        || !(discoveryTestcaseCredKeyInfo = discoveryTestcaseCredInfo.getKeyDescriptor()).hasPrivateKey()
                        || !discoveryTestcaseCredInfo.hasCertificateDescriptor()
                        || !(discoveryTestcaseCredCertInfo = discoveryTestcaseCredInfo.getCertificateDescriptor()).hasCertificate()) {
                        continue;
                    }

                    // noinspection ConstantConditions
                    if ((decryptedBodyPart =
                        ToolSmimeUtils.decrypt(msgHelper, enveloped, discoveryTestcaseCredKeyInfo.getPrivateKey(),
                            discoveryTestcaseCredCertInfo.getCertificate())) != null) {
                        result.setCredentialFound(discoveryTestcaseCred);

                        LOGGER.info(String.format("Decrypted enveloped mail MIME message (id=%s, from=%s, to=%s) enveloped content (type=%s).", msgId, msgFrom,
                            msgTo, ToolMimePartUtils.getContentType(enveloped.getEncryptedContent())));

                        break;
                    }
                }
            }

            if (decryptedBodyPart == null) {
                throw new ToolSmimeException(String.format(
                    "Unable to decrypt enveloped mail MIME message (id=%s, from=%s, to=%s) enveloped content (type=%s).", msgId, msgFrom, msgTo,
                    ToolMimePartUtils.getContentType(enveloped.getEncryptedContent())));
            }

            SMIMESigned signed = ToolSmimeUtils.getSigned(msgHelper, decryptedBodyPart);

            LOGGER.info(String.format("Extracted mail MIME message (id=%s, from=%s, to=%s) signed content (type=%s).", msgId, msgFrom, msgTo,
                ToolMimePartUtils.getContentType(signed.getContent())));

            Map<SignerId, Pair<SignerInformation, CertificateInfo>> signerInfoCertMap = ToolSmimeUtils.verifySignatures(signed);

            if (signerInfoCertMap.isEmpty()) {
                throw new ToolSmimeException(String.format(
                    "Mail MIME message (id=%s, from=%s, to=%s) signed content (type=%s) does not contain any signed parts.", msgId, msgFrom, msgTo,
                    ToolMimePartUtils.getContentType(signed.getContent())));
            }

            ToolTestcaseStep senderCertDiscoveryLastStep = this.discoverSenderCertificate(msgFrom);
            ToolTestcaseCertificateStep senderCertDiscoveryCertStep;
            CertificateInfo senderCertInfo;

            // noinspection ConstantConditions
            if (!senderCertDiscoveryLastStep.isSuccessful()
                || !ToolClassUtils.isAssignable(senderCertDiscoveryLastStep.getClass(), ToolTestcaseCertificateStep.class)
                || !(senderCertDiscoveryCertStep = ((ToolTestcaseCertificateStep) senderCertDiscoveryLastStep)).hasCertificateInfo()
                || !(senderCertInfo = senderCertDiscoveryCertStep.getCertificateInfo()).hasCertificate()) {
                throw new ToolSmimeException(String.format("Unable to discover mail MIME message (id=%s, from=%s, to=%s) sender certificate: %s", msgId,
                    msgFrom, msgTo, senderCertDiscoveryLastStep.getMessage()));
            }

            X509Certificate senderCert = senderCertInfo.getCertificate();
            // noinspection ConstantConditions
            CertificateName senderCertIssuerName = new CertificateNameImpl(senderCert.getIssuerX500Principal());
            SignerId senderSignerId = new SignerId(senderCertIssuerName.toX500Name(), senderCert.getSerialNumber());

            if (!signerInfoCertMap.containsKey(senderSignerId)) {
                throw new ToolSmimeException(
                    String
                        .format(
                            "Mail MIME message (id=%s, from=%s, to=%s) signed content (type=%s) was not signed by the discovered sender certificate (subj={%s}, issuer={%s}, serialNum=%s).",
                            msgId, msgFrom, msgTo, ToolMimePartUtils.getContentType(signed.getContent()), senderCertInfo.getSubject(), senderCertIssuerName,
                            senderCertInfo.getSerialNumber()));
            }

            // noinspection ConstantConditions
            LOGGER
                .info(String
                    .format(
                        "Found a matching signer for discovered sender certificate (subj={%s}, issuer={%s}, serialNum=%s) in mail MIME message (id=%s, from=%s, to=%s) signed content (type=%s).",
                        senderCertInfo.getSubject(), senderCertIssuerName, senderCertInfo.getSerialNumber(), msgId, msgFrom, msgTo,
                        ToolMimePartUtils.getContentType(signed.getContent())));

            ToolTestcaseCertificateResultType senderCertValidityStatus =
                ToolTestcaseCertificateUtils.validateCertificate(senderCertInfo, msgFrom, this.certValidators);

            if (senderCertValidityStatus != ToolTestcaseCertificateResultType.VALID_CERT) {
                throw new ToolSmimeException(String.format(
                    "Mail MIME message (id=%s, from=%s, to=%s) signed content (type=%s) signer certificate is invalid: %s", msgId, msgFrom, msgTo,
                    ToolMimePartUtils.getContentType(signed.getContent()), senderCertValidityStatus.getMessage()));
            }
        } catch (Exception e) {
            decryptionErrorBuilder.appendWithDelimiter(e.getMessage(), StringUtils.LF);
        }

        // noinspection ConstantConditions
        mailInfo.setDecryptionErrorMessage(decryptionErrorBuilder.build());

        return result;
    }

    @Nullable
    private ToolTestcaseStep discoverSenderCertificate(MailAddress msgFrom) throws ToolTestcaseResultException {
        return ToolListUtils.getLast(this.certDiscoveryService.discoverCertificates(msgFrom));
    }
}
