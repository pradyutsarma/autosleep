Feature: public paas service consummer org and space autoenrollment

  A service consummer consumming a public Paas, wishes to optimize its non-prod applications usage on the public paas.
  The service consummer has org admin role on one of more organization, but has not cloudcontroller.admin permission on
  the CF instance.


  Background:
    Given autosleep is deployed by service provider with the following configuration flags
      """
      # the username of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.username=
      #the password of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
      cf.client.password=

      #one of: disabled, standard (opt-out possible), forced (only transient opt-out)
      enrollspace.mode=forced
      # Periodicity of the space enrollment
      enrollspace.service.instance.name=autosleep-autoenrolled
      # Periodicity of the space enrollment (and transient opt-out)
      enrollspace.perodicity=T10M

      # Indicates whether the CC API user has cloudcontroller.admin permissions. If not, it should be org admin of each of the orgs to enroll
      enrollspace.cf.client.is-cc-admin=false

      # Include or exclude orgs and space from enrollment using regular expressions
      enrollspace.include-orgs=*
      enrollspace.exclude-orgs=(*prod)
      enrollspace.include-space=*
      enrollspace.exclude-space=prod*
      #Exclude shared from autowakeup registration
      enrollspace.exclude-domain=portal.acme.com

      """


  Scenario: newly autoenrolled spaces get added the autosleep user without cloudcontroller.admin as space developer

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org           | spaces    |
      | team-b        | dev, prod |
    And the autosleep service instances and autosleep user space roles in each space are
      | org           | space     | autosleep service instances (arbitrary params) | autosleep user role |
      | team-b        | dev       |                                                |                     |
      | team-b        | prod      |                                                |                     |

    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances and autosleep user space roles in each space are
      | org           | space     | autosleep service instances (arbitrary params)                       | autosleep user role |
      | team-b        | dev       | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) | space-dev           |
      | team-b        | prod      |                                                                      |                     |

  Scenario: previously autoenrolled space autoenrollment re-adds autosleep user without cloudcontroller.admin as space developer if missing

    Given a CF instance with the following orgs, spaces (visible to the autosleep user)
      | org           | spaces    |
      | team-b        | dev, prod |
    And the autosleep service instances and autosleep user space roles in each space are
      | org           | space     | autosleep service instances (arbitrary params)                       | autosleep user role |
      | team-b        | dev       | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) |                     |
      | team-b        | prod      |                                                                      |                     |

    When the clock reaches the scan date
    Then autosleep periodically automatically scans orgs and spaces
    And the autosleep service instances and autosleep user space roles in each space are
      | org           | space     | autosleep service instances (arbitrary params)                       | autosleep user role |
      | team-b        | dev       | autosleep-autoenrolled(idle-duration=T10H, auto-enrollment=standard) | space-dev           |
      | team-b        | prod      |                                                                      |                     |


