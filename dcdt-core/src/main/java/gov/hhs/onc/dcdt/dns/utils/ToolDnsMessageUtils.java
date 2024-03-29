package gov.hhs.onc.dcdt.dns.utils;

import gov.hhs.onc.dcdt.dns.DnsMessageFlag;
import gov.hhs.onc.dcdt.dns.DnsMessageOpcode;
import gov.hhs.onc.dcdt.dns.DnsMessageRcode;
import gov.hhs.onc.dcdt.dns.DnsMessageSection;
import gov.hhs.onc.dcdt.net.InetProtocol;
import gov.hhs.onc.dcdt.utils.ToolArrayUtils;
import gov.hhs.onc.dcdt.utils.ToolNumberUtils;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Record;

public abstract class ToolDnsMessageUtils {
    public final static int DATA_SIZE_DNS_MSG_QUERY_SIZE_PREFIX = 2;

    public static Message createErrorResponse(@Nullable Message reqMsg, DnsMessageRcode rcode) {
        return setRcode(createResponse(ObjectUtils.defaultIfNull(reqMsg, new Message())), rcode);
    }

    public static Message createResponse(Message reqMsg) {
        return copyRecords(reqMsg, setFlags(copyFlags(reqMsg, new Message(reqMsg.getHeader().getID()), DnsMessageFlag.RD), DnsMessageFlag.QR),
            DnsMessageSection.QUESTION);
    }

    public static byte[] toWire(InetProtocol protocol, Message msg) throws IOException {
        byte[] data = msg.toWire(protocol.getDataSizeMax());

        return ((protocol == InetProtocol.TCP) ? ArrayUtils.addAll(buildQuerySizeData(data.length), data) : data);
    }

    public static Message fromWire(InetProtocol protocol, byte[] data) throws IOException {
        return new Message(((protocol == InetProtocol.TCP) ? ArrayUtils.subarray(data, DATA_SIZE_DNS_MSG_QUERY_SIZE_PREFIX,
            (parseQuerySizeData(ArrayUtils.subarray(data, 0, DATA_SIZE_DNS_MSG_QUERY_SIZE_PREFIX))) + DATA_SIZE_DNS_MSG_QUERY_SIZE_PREFIX) : data));
    }

    public static byte[] buildQuerySizeData(@Nonnegative int querySize) {
        return ArrayUtils.toPrimitive(ArrayUtils.toArray(((byte) ((querySize >>> 8) & 0xFF)), ((byte) (querySize & 0xFF))));
    }

    @Nonnegative
    public static int parseQuerySizeData(byte ... querySizePrefixData) {
        return (((querySizePrefixData[0] & 0xFF) << 8) + (querySizePrefixData[1] & 0xFF));
    }

    public static Message setAdditional(Message msg, @Nullable Record ... additionalRecords) {
        return setAdditional(msg, ToolArrayUtils.asList(additionalRecords));
    }

    public static Message setAdditional(Message msg, @Nullable Iterable<? extends Record> additionalRecords) {
        return setRecords(msg, DnsMessageSection.ADDITIONAL, additionalRecords);
    }

    public static Message setAuthorities(Message msg, boolean authoritative, @Nullable Record ... authorityRecords) {
        return setAuthorities(msg, authoritative, ToolArrayUtils.asList(authorityRecords));
    }

    public static Message setAuthorities(Message msg, boolean authoritative, @Nullable Iterable<? extends Record> authorityRecords) {
        return setRecords((authoritative ? setFlags(msg, DnsMessageFlag.AA) : msg), DnsMessageSection.AUTHORITY, authorityRecords);
    }

    public static Message setAnswers(Message msg, @Nullable Record ... answerRecords) {
        return setAnswers(msg, ToolArrayUtils.asList(answerRecords));
    }

    public static Message setAnswers(Message msg, @Nullable Iterable<? extends Record> answerRecords) {
        return (!hasRecords(setRecords(msg, DnsMessageSection.ANSWER, answerRecords), DnsMessageSection.ANSWER) ? setRcode(msg, DnsMessageRcode.NOERROR) : msg);
    }

    @Nullable
    public static DnsMessageOpcode getOpcode(Message msg) {
        return ToolDnsUtils.findByCode(DnsMessageOpcode.class, msg.getHeader().getOpcode());
    }

    public static Message setOpcode(Message msg, DnsMessageOpcode opcode) {
        msg.getHeader().setOpcode(opcode.getCode());

        return msg;
    }

    @Nullable
    public static DnsMessageRcode getRcode(Message msg) {
        return ToolDnsUtils.findByCode(DnsMessageRcode.class, msg.getRcode());
    }

    public static Message setRcode(Message msg, DnsMessageRcode rcode) {
        msg.getHeader().setRcode(rcode.getCode());

        return msg;
    }

    public static boolean hasRecords(Message msg, DnsMessageSection section) {
        return ToolNumberUtils.isPositive(msg.getHeader().getCount(section.getCode()));
    }

    public static Message copyRecords(Message msg1, Message msg2, DnsMessageSection section) {
        return addRecords(msg2, section, msg1.getSectionArray(section.getCode()));
    }

    public static Message setRecords(Message msg, DnsMessageSection section, @Nullable Record ... records) {
        return setRecords(msg, section, ToolArrayUtils.asList(records));
    }

    public static Message setRecords(Message msg, DnsMessageSection section, @Nullable Iterable<? extends Record> records) {
        msg.removeAllRecords(section.getCode());

        return addRecords(msg, section, records);
    }

    public static Message addRecords(Message msg, DnsMessageSection section, @Nullable Record ... records) {
        return addRecords(msg, section, ToolArrayUtils.asList(records));
    }

    public static Message addRecords(Message msg, DnsMessageSection section, @Nullable Iterable<? extends Record> records) {
        if (records != null) {
            for (Record record : records) {
                msg.addRecord(record, section.getCode());
            }
        }

        return msg;
    }

    public static Message removeRecords(Message msg, DnsMessageSection section, @Nullable Record ... records) {
        return removeRecords(msg, section, ToolArrayUtils.asList(records));
    }

    public static Message removeRecords(Message msg, DnsMessageSection section, @Nullable Iterable<? extends Record> records) {
        if (records != null) {
            for (Record record : records) {
                msg.removeRecord(record, section.getCode());
            }
        }

        return msg;
    }

    public static Message copyFlags(Message msg1, Message msg2, @Nullable DnsMessageFlag ... flags) {
        return copyFlags(msg1, msg2, ToolArrayUtils.asList(flags));
    }

    public static Message copyFlags(Message msg1, Message msg2, @Nullable Iterable<DnsMessageFlag> flags) {
        Set<DnsMessageFlag> flagsSet = ((flags != null) ? new HashSet<>(IteratorUtils.toList(flags.iterator())) : EnumSet.allOf(DnsMessageFlag.class));
        Header header1 = msg1.getHeader(), header2 = msg2.getHeader();
        int flagCode;
        boolean flagValue;

        for (DnsMessageFlag flag : flagsSet) {
            if ((flagValue = header1.getFlag((flagCode = flag.getCode()))) != header2.getFlag(flagCode)) {
                if (flagValue) {
                    header2.setFlag(flagCode);
                } else {
                    header2.unsetFlag(flagCode);
                }
            }
        }

        return msg2;
    }

    public static boolean hasFlags(Message msg, @Nullable DnsMessageFlag ... flags) {
        return hasFlags(msg, ToolArrayUtils.asList(flags));
    }

    public static boolean hasFlags(Message msg, @Nullable Iterable<DnsMessageFlag> flags) {
        if (flags != null) {
            Header header = msg.getHeader();
            OPTRecord optRecord = msg.getOPT();

            for (DnsMessageFlag flag : flags) {
                if ((!flag.isExtended() && !header.getFlag(flag.getCode()))
                    || (flag.isExtended() && ((optRecord == null) || ((optRecord.getFlags() & flag.getCode()) == 0)))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static Set<DnsMessageFlag> getFlags(Message msg) {
        Header header = msg.getHeader();
        OPTRecord optRecord = msg.getOPT();
        Set<DnsMessageFlag> flags = EnumSet.noneOf(DnsMessageFlag.class);

        for (DnsMessageFlag flag : EnumSet.allOf(DnsMessageFlag.class)) {
            if ((!flag.isExtended() && header.getFlag(flag.getCode()))
                || (flag.isExtended() && (optRecord != null) && ((optRecord.getFlags() & flag.getCode()) != 0))) {
                flags.add(flag);
            }
        }

        return flags;
    }

    public static Message setFlags(Message msg, @Nullable DnsMessageFlag ... flags) {
        return setFlags(msg, ToolArrayUtils.asList(flags));
    }

    public static Message setFlags(Message msg, @Nullable Iterable<DnsMessageFlag> flags) {
        if (flags != null) {
            Header header = msg.getHeader();
            OPTRecord optRecord = msg.getOPT();

            for (DnsMessageFlag flag : flags) {
                if (!flag.isExtended()) {
                    header.setFlag(flag.getCode());
                } else if (optRecord != null) {
                    removeRecords(msg, DnsMessageSection.ADDITIONAL, optRecord);
                    addRecords(msg, DnsMessageSection.ADDITIONAL, (optRecord =
                        new OPTRecord(optRecord.getPayloadSize(), optRecord.getExtendedRcode(), optRecord.getVersion(),
                            (optRecord.getFlags() | flag.getCode()), optRecord.getOptions())));
                } else {
                    addRecords(msg, DnsMessageSection.ADDITIONAL, (optRecord = new OPTRecord(0xff, 0, 0, flag.getCode())));
                }
            }
        }

        return msg;
    }
}
