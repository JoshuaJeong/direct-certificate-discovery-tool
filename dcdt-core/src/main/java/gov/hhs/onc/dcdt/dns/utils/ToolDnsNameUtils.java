package gov.hhs.onc.dcdt.dns.utils;

import gov.hhs.onc.dcdt.dns.DnsNameException;
import gov.hhs.onc.dcdt.dns.DnsNameLabel;
import gov.hhs.onc.dcdt.utils.ToolArrayUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.TextParseException;

public abstract class ToolDnsNameUtils {
    @Nullable
    public static <T extends Enum<T> & DnsNameLabel> T findByLabel(Class<T> nameLblEnumClass, Name nameLbl) {
        return findByLabelString(nameLblEnumClass, nameLbl.toString());
    }

    @Nullable
    public static <T extends Enum<T> & DnsNameLabel> T findByLabelString(Class<T> nameLblEnumClass, String nameLblStr) {
        for (T nameLblEnum : EnumSet.allOf(nameLblEnumClass)) {
            if (Objects.equals(nameLblEnum.getDnsNameLabelString(), nameLblStr)) {
                return nameLblEnum;
            }
        }

        return null;
    }

    public static Name toAbsolute(Name name) throws DnsNameException {
        return name.isAbsolute() ? name : fromLabels(name, Name.root);
    }

    public static List<Name> toLabels(Name name) throws DnsNameException {
        int numNameLbls = name.labels();
        List<Name> nameLbls = new ArrayList<>(numNameLbls);

        for (int a = 0; a < numNameLbls; a++) {
            nameLbls.add(fromLabelStrings(name.getLabelString(a)));
        }

        return nameLbls;
    }

    public static List<String> toLabelStrings(Name name) {
        int numNameLbls = name.labels();
        List<String> nameLblStrs = new ArrayList<>(numNameLbls);

        for (int a = 0; a < numNameLbls; a++) {
            nameLblStrs.add(name.getLabelString(a));
        }

        return nameLblStrs;
    }

    public static Name fromLabelStrings(String ... nameLblStrs) throws DnsNameException {
        return fromLabelStrings(ToolArrayUtils.asList(nameLblStrs));
    }

    public static Name fromLabelStrings(Iterable<String> nameLblStrs) throws DnsNameException {
        List<Name> nameLblNames = new ArrayList<>();

        for (String nameLblStr : nameLblStrs) {
            nameLblNames.add(fromString(nameLblStr));
        }

        return fromLabels(nameLblNames);
    }

    public static Name fromLabels(Name ... nameLbls) throws DnsNameException {
        return fromLabels(ToolArrayUtils.asList(nameLbls));
    }

    public static Name fromLabels(Iterable<Name> nameLbls) throws DnsNameException {
        Name name = null;

        try {
            for (Name nameLbl : nameLbls) {
                name = (name != null) ? ((nameLbl != null) ? Name.concatenate(name, nameLbl) : name) : nameLbl;
            }
        } catch (NameTooLongException e) {
            throw new DnsNameException("Unable to build DNS name from labels.", e);
        }

        return (name != null) ? name : Name.empty;
    }

    public static Name fromString(@Nullable String nameStr) throws DnsNameException {
        try {
            return (nameStr != null) ? Name.fromString(nameStr) : Name.empty;
        } catch (TextParseException e) {
            throw new DnsNameException(String.format("Unable to get DNS name from string: %s", nameStr), e);
        }
    }
}