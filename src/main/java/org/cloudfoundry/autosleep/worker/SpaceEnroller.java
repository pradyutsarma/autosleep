package org.cloudfoundry.autosleep.worker;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;
import org.cloudfoundry.autosleep.worker.scheduling.AbstractPeriodicTask;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
class SpaceEnroller extends AbstractPeriodicTask {


    private final String spaceEnrollerConfigId;

    private final CloudFoundryApiService cloudFoundryApi;

    private final SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    private final ApplicationRepository applicationRepository;

    private DeployedApplicationConfig.Deployment deployment;

    @Builder
    SpaceEnroller(Clock clock, Duration period, String spaceEnrollerConfigId,
                  CloudFoundryApiService cloudFoundryApi, SpaceEnrollerConfigRepository spaceEnrollerConfigRepository,
                  ApplicationRepository applicationRepository,
                  DeployedApplicationConfig.Deployment deployment) {
        super(clock, period);
        this.spaceEnrollerConfigId = spaceEnrollerConfigId;
        this.cloudFoundryApi = cloudFoundryApi;
        this.spaceEnrollerConfigRepository = spaceEnrollerConfigRepository;
        this.applicationRepository = applicationRepository;
        this.deployment = deployment;
    }

    @Override
    public void run() {
        SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository.findOne(spaceEnrollerConfigId);
        if (serviceInstance != null) {
            try {
                Set<String> watchedOrIgnoredApplications = new HashSet<>();
                applicationRepository.findAll()
                        .forEach(applicationInfo -> {
                            if (!applicationInfo.getEnrollmentState().isCandidate(spaceEnrollerConfigId)) {
                                watchedOrIgnoredApplications.add(applicationInfo.getUuid());
                            }
                        });
                log.debug("{} known applications (already enrolled, or blacklisted)",
                        watchedOrIgnoredApplications.size());
                List<ApplicationIdentity> applicationIdentities = cloudFoundryApi
                        .listApplications(serviceInstance.getSpaceId(),
                                serviceInstance.getExcludeFromAutoEnrollment());
                List<ApplicationIdentity> newApplications = applicationIdentities.stream()
                        .filter(application ->
                                deployment == null || !deployment.getApplicationId().equals(application.getGuid()))
                        .filter(application -> !(watchedOrIgnoredApplications.contains(application.getGuid())))
                        .collect(Collectors.toList());
                if (!newApplications.isEmpty()) {
                    log.debug("{} - new applications", newApplications.size());
                    cloudFoundryApi.bindServiceInstance(newApplications, serviceInstance.getId());
                } else {
                    log.debug("No new app to bind (all already enrolled or filtered by regexp)");
                }
            } catch (CloudFoundryException c) {
                /*a 409 "conflict" error is possible (if someone tries to delete a service at the same time
                but without consequences
                 */
                log.error("remote error", c);
            }
            rescheduleWithDefaultPeriod();
        } else {
            log.debug("service has been removed. Cancelling task");
            stopTask();
        }
    }

    @Override
    protected String getTaskId() {
        return spaceEnrollerConfigId;
    }
}
