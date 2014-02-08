package gov.hhs.onc.dcdt.testcases.hosting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import gov.hhs.onc.dcdt.beans.NamedBeanName;
import gov.hhs.onc.dcdt.json.ToolBeanJsonDto;
import gov.hhs.onc.dcdt.mail.DirectAddress;
import gov.hhs.onc.dcdt.testcases.hosting.impl.HostingTestcaseSubmissionJsonDtoImpl;
import javax.annotation.Nullable;

@JsonSubTypes({ @Type(HostingTestcaseSubmissionJsonDtoImpl.class) })
public interface HostingTestcaseSubmissionJsonDto extends ToolBeanJsonDto<HostingTestcaseSubmission> {
    public boolean hasHostingTestcase();

    @JsonProperty("hostingTestcase")
    @NamedBeanName(HostingTestcase.class)
    @Nullable
    public String getHostingTestcase();

    public void setHostingTestcase(@Nullable String hostingTestcase);

    public boolean hasDirectAddress();

    @DirectAddress
    @JsonProperty("directAddr")
    @Nullable
    public String getDirectAddress();

    public void setDirectAddress(@Nullable String directAddr);
}