package gov.hhs.onc.dcdt.service.dns.server.impl;

import gov.hhs.onc.dcdt.config.InstanceDnsConfig;
import gov.hhs.onc.dcdt.dns.DnsException;
import gov.hhs.onc.dcdt.dns.DnsMessageFlag;
import gov.hhs.onc.dcdt.dns.DnsMessageRcode;
import gov.hhs.onc.dcdt.dns.DnsRecordType;
import gov.hhs.onc.dcdt.dns.utils.ToolDnsMessageUtils;
import gov.hhs.onc.dcdt.dns.utils.ToolDnsRecordUtils;
import gov.hhs.onc.dcdt.net.InetProtocol;
import gov.hhs.onc.dcdt.nio.channels.impl.AbstractChannelListenerDataProcessor;
import gov.hhs.onc.dcdt.service.dns.config.DnsServerConfig;
import gov.hhs.onc.dcdt.service.dns.server.DnsServerQueryResolutionException;
import gov.hhs.onc.dcdt.service.dns.server.DnsServerQueryResolver;
import gov.hhs.onc.dcdt.utils.ToolClassUtils;
import java.util.Collection;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

@Component("dnsServerQueryResolverImpl")
@Lazy
@Scope("prototype")
@SuppressWarnings({ "SpringJavaAutowiringInspection" })
public class DnsServerQueryResolverImpl extends AbstractChannelListenerDataProcessor implements DnsServerQueryResolver {
    private DnsServerConfig dnsServerConfig;

    private final static Logger LOGGER = LoggerFactory.getLogger(DnsServerQueryResolverImpl.class);

    public DnsServerQueryResolverImpl(DnsServerConfig dnsServerConfig, InetProtocol protocol, byte[] reqData) {
        super(protocol, reqData);

        this.dnsServerConfig = dnsServerConfig;
    }

    @Override
    public byte[] call() throws Exception {
        Message reqMsg = null, respMsg = null;
        byte[] respData = null;

        try {
            respData = (respMsg = this.resolveQuery((reqMsg = new Message(this.reqData)))).toWire();

            if (respData.length > this.protocol.getDataSizeMax()) {
                ToolDnsMessageUtils.setFlags(respMsg, DnsMessageFlag.TC);

                respData = respMsg.toWire();
            }

            LOGGER.trace(String.format("Resolved (class=%s) DNS server query (protocol=%s, reqDataSize=%d, reqMsg={%s}): respDataSize=%d, respMsg={%s}",
                ToolClassUtils.getName(this), this.protocol.name(), this.reqData.length, reqMsg, ArrayUtils.getLength(respData), respMsg));

            if (this.protocol == InetProtocol.TCP) {
                int respDataSize = respData.length;

                return ArrayUtils.addAll(ArrayUtils.toPrimitive(ArrayUtils.toArray(((byte) ((respDataSize >>> 8) & 0xFF)), ((byte) (respDataSize & 0xFF)))),
                    respData);
            } else {
                return respData;
            }
        } catch (Exception e) {
            throw new DnsServerQueryResolutionException(reqMsg, respMsg, String.format(
                "Unable to resolve (class=%s) DNS server query (protocol=%s, reqDataSize=%d, reqMsg={%s}): respDataSize=%d, respMsg={%s}",
                ToolClassUtils.getName(this), this.protocol.name(), this.reqData.length, reqMsg, ArrayUtils.getLength(respData), respMsg), e);
        }
    }

    private Message resolveQuery(Message reqMsg) throws DnsException {
        Message respMsg = ToolDnsMessageUtils.createResponse(reqMsg);
        Record questionRecord = reqMsg.getQuestion();

        if (questionRecord != null) {
            DnsRecordType questionRecordType = ToolDnsRecordUtils.findByType(questionRecord.getType());

            if (questionRecordType != null) {
                Collection<Record> answerRecords = null;
                InstanceDnsConfig authoritativeDnsConfig = this.dnsServerConfig.findAuthoritativeDnsConfig(questionRecord);

                if (authoritativeDnsConfig != null) {
                    answerRecords = authoritativeDnsConfig.findAnswers(questionRecord);
                    // noinspection ConstantConditions
                    ToolDnsMessageUtils.setAuthorities(respMsg, true, authoritativeDnsConfig.getSoaRecordConfig().toRecord());
                }

                ToolDnsMessageUtils.setAnswers(respMsg, answerRecords);
            } else {
                ToolDnsMessageUtils.setRcode(respMsg, DnsMessageRcode.NXRRSET);
            }
        } else {
            ToolDnsMessageUtils.setRcode(respMsg, DnsMessageRcode.REFUSED);
        }

        return respMsg;
    }
}