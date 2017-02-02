package org.cloudfoundry.autosleep.access.dao.model;

import java.time.Duration;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.cloudfoundry.autosleep.config.Config.OrgEnrollmentParameters;
import org.cloudfoundry.autosleep.config.Config.ServiceInstanceParameters;
import org.cloudfoundry.autosleep.util.serializer.PatternDeserializer;
import org.cloudfoundry.autosleep.util.serializer.PatternSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(of = {"organizationId"})
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrolledOrganizationConfig {
    
    @Id
    @JsonProperty
    private String organizationId;
    
    @JsonProperty
    private Duration idleDuration;
    
    @JsonSerialize(using = PatternSerializer.class)
    @JsonDeserialize(using = PatternDeserializer.class)
    @Lob
    @Column
    @JsonProperty
    private Pattern excludeFromAutoEnrollment;
    
    @Enumerated(EnumType.ORDINAL)
    private OrgEnrollmentParameters.EnrolledState state;
    
    @Enumerated(EnumType.ORDINAL)
    private ServiceInstanceParameters.Enrollment autoEnrollment;
    
}
