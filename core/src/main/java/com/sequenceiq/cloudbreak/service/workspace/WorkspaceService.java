package com.sequenceiq.cloudbreak.service.workspace;

import static com.sequenceiq.cloudbreak.api.model.v2.WorkspaceStatus.DELETED;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.WORKSPACE_MANAGE;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.users.ChangeWorkspaceUsersJson;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class WorkspaceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private WorkspaceRepository workspaceRepository;

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceModificationVerifierService workspaceModificationVerifierService;

    public Workspace create(User user, Workspace workspace) {
        try {
            return transactionService.required(() -> {
                Workspace createdWorkspace = workspaceRepository.save(workspace);
                UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
                userWorkspacePermissions.setWorkspace(createdWorkspace);
                userWorkspacePermissions.setUser(user);
                userWorkspacePermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), WORKSPACE_MANAGE.value()));
                userWorkspacePermissionsService.save(userWorkspacePermissions);
                return createdWorkspace;
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Workspace with name '%s' in your tenant already exists.",
                        workspace.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Workspace> retrieveForUser(User user) {
        return userWorkspacePermissionsService.findForUser(user).stream()
                .map(UserWorkspacePermissions::getWorkspace).collect(Collectors.toSet());
    }

    public Workspace getDefaultWorkspaceForUser(User user) {
        return workspaceRepository.getByName(user.getUserId(), user.getTenant());
    }

    public Optional<Workspace> getByName(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(name, user.getTenant()));
    }

    public Optional<Workspace> getByNameForUser(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(name, user.getTenant()));
    }

    public Workspace getById(Long id) {
        Optional<Workspace> workspace = workspaceRepository.findById(id);
        if (workspace.isPresent()) {
            return workspace.get();
        }
        throw new IllegalArgumentException(String.format("No Workspace found with id: %s", id));
    }

    public Workspace get(Long id, User user) {
        UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserByWorkspaceId(user, id);
        if (userWorkspacePermissions == null) {
            throw new NotFoundException("Cannot find workspace by user.");
        }
        return userWorkspacePermissions.getWorkspace();
    }

    public Set<User> removeUsers(String orgName, Set<String> userIds, User user) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = getByNameForUserOrThrowNotFound(orgName, user);
                authorizeWorkspaceManipulation(user, workspace, Action.MANAGE);

                Set<User> users = userService.getByUsersIds(userIds);
                Set<UserWorkspacePermissions> toBeRemoved = validateAllUsersAreAlreadyInTheWorkspace(workspace, users);
                Set<User> usersToBeRemoved = toBeRemoved.stream().map(UserWorkspacePermissions::getUser).collect(Collectors.toSet());
                workspaceModificationVerifierService.verifyRemovalFromDefaultWorkspace(user, workspace, usersToBeRemoved);

                userWorkspacePermissionsService.deleteAll(toBeRemoved);
                return toBeRemoved.stream()
                        .map(UserWorkspacePermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> addUsers(String orgName, Set<ChangeWorkspaceUsersJson> changeWorkspaceUsersJsons, User currentUser) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeWorkspaceManipulation(currentUser, workspace, Action.INVITE);

                Map<User, Set<String>> usersToAddWithPermissions = orgUserPermissionJsonSetToMap(changeWorkspaceUsersJsons);
                validateUsersAreNotInTheWorkspaceYet(workspace, usersToAddWithPermissions.keySet());

                Set<UserWorkspacePermissions> userWorkspacePermsToAdd = usersToAddWithPermissions.entrySet().stream()
                        .map(userWithPermissions -> {
                            UserWorkspacePermissions newUserPermission = new UserWorkspacePermissions();
                            newUserPermission.setPermissionSet(userWithPermissions.getValue());
                            newUserPermission.setUser(userWithPermissions.getKey());
                            newUserPermission.setWorkspace(workspace);
                            return newUserPermission;
                        })
                        .collect(Collectors.toSet());

                userWorkspacePermissionsService.saveAll(userWorkspacePermsToAdd);
                return userWorkspacePermsToAdd.stream().map(UserWorkspacePermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> updateUsers(String orgName, Set<ChangeWorkspaceUsersJson> updateWorkspaceUsersJsons, User currentUser) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeWorkspaceManipulation(currentUser, workspace, Action.MANAGE);

                Map<User, Set<String>> usersToUpdateWithPermissions = orgUserPermissionJsonSetToMap(updateWorkspaceUsersJsons);
                Map<User, UserWorkspacePermissions> toBeUpdated = validateAllUsersAreAlreadyInTheWorkspace(
                        workspace, usersToUpdateWithPermissions.keySet()).stream()
                        .collect(Collectors.toMap(UserWorkspacePermissions::getUser, uop -> uop));

                Set<UserWorkspacePermissions> userWorkspacePermissions = toBeUpdated.entrySet().stream()
                        .map(userPermission -> {
                            userPermission.getValue().setPermissionSet(usersToUpdateWithPermissions.get(userPermission.getKey()));
                            return userPermission.getValue();
                        })
                        .collect(Collectors.toSet());

                Set<User> usersToBeUpdated = userWorkspacePermissions.stream().map(UserWorkspacePermissions::getUser).collect(Collectors.toSet());
                workspaceModificationVerifierService.verifyUserUpdates(currentUser, workspace, usersToBeUpdated);

                userWorkspacePermissionsService.saveAll(userWorkspacePermissions);
                return toBeUpdated.keySet();
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace getByNameForUserOrThrowNotFound(String orgName, User currentUser) {
        Optional<Workspace> workspace = getByNameForUser(orgName, currentUser);
        return workspace.orElseThrow(() -> new NotFoundException("Cannot find workspace with name: " + orgName));
    }

    public Set<User> changeUsers(String orgName, Map<String, Set<String>> userPermissions, User currentUser) {
        try {
            return transactionService.required(() -> {
                Workspace workspace = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeWorkspaceManipulation(currentUser, workspace, Action.MANAGE);

                Set<UserWorkspacePermissions> oldPermissions = userWorkspacePermissionsService.findForWorkspace(workspace);
                Set<User> oldUsers = oldPermissions.stream().map(UserWorkspacePermissions::getUser).collect(Collectors.toSet());

                workspaceModificationVerifierService.verifyUserUpdates(currentUser, workspace, oldUsers);
                userWorkspacePermissionsService.deleteAll(oldPermissions);

                Map<String, User> usersToAdd = userService.getByUsersIds(userPermissions.keySet()).stream()
                        .collect(Collectors.toMap(User::getUserId, user -> user));

                userPermissions.entrySet().stream()
                        .map(userPermission -> {
                            User user = usersToAdd.get(userPermission.getKey());
                            UserWorkspacePermissions newUserPermission = new UserWorkspacePermissions();
                            newUserPermission.setPermissionSet(userPermission.getValue());
                            newUserPermission.setUser(user);
                            newUserPermission.setWorkspace(workspace);
                            return newUserPermission;
                        })
                        .forEach(userWorkspacePermissionsService::save);

                return new HashSet<>(usersToAdd.values());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace deleteByNameForUser(String orgName, User currentUser, Workspace defaultWorkspace) {
        try {
            return transactionService.required(() -> {
                Workspace workspaceForDelete = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeWorkspaceManipulation(currentUser, workspaceForDelete, Action.MANAGE);

                workspaceModificationVerifierService.checkThatWorkspaceIsDeletable(currentUser, workspaceForDelete, defaultWorkspace);
                Long deleted = userWorkspacePermissionsService.deleteByWorkspace(workspaceForDelete);
                setupDeletionDateAndFlag(workspaceForDelete);
                workspaceRepository.save(workspaceForDelete);
                LOGGER.info("Deleted organisation: {}, related permissions: {}", orgName, deleted);
                return workspaceForDelete;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Map<User, Set<String>> orgUserPermissionJsonSetToMap(Set<ChangeWorkspaceUsersJson> updateWorkspaceUsersJsons) {
        return updateWorkspaceUsersJsons.stream()
                .collect(Collectors.toMap(json -> userService.getByUserId(json.getUserId()), json -> json.getPermissions()));
    }

    private Set<UserWorkspacePermissions> validateAllUsersAreAlreadyInTheWorkspace(Workspace workspace, Set<User> users) {
        validateAllUsersAreInTheTenant(workspace, users);
        Set<String> usersNotInTheWorkspace = new TreeSet<>();

        Set<UserWorkspacePermissions> userWorkspacePermissionsSet = users.stream()
                .map(user -> {
                    UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserAndWorkspace(user, workspace);
                    if (userWorkspacePermissions == null) {
                        usersNotInTheWorkspace.add(user.getUserId());
                    }
                    return userWorkspacePermissions;
                })
                .collect(Collectors.toSet());

        if (!usersNotInTheWorkspace.isEmpty()) {
            String usersCommaSeparated = usersNotInTheWorkspace.stream().collect(Collectors.joining(", "));
            throw new BadRequestException("The following users are not in the workspace: " + usersCommaSeparated);
        }

        return userWorkspacePermissionsSet;
    }

    private void validateUsersAreNotInTheWorkspaceYet(Workspace workspace, Set<User> users) {
        validateAllUsersAreInTheTenant(workspace, users);

        Set<String> usersInWorkspace = users.stream()
                .filter(user -> userWorkspacePermissionsService.findForUserAndWorkspace(user, workspace) != null)
                .map(User::getUserId)
                .collect(Collectors.toSet());

        if (!usersInWorkspace.isEmpty()) {
            String usersCommaSeparated = usersInWorkspace.stream().collect(Collectors.joining(", "));
            throw new BadRequestException("The following users are already in the workspace: " + usersCommaSeparated);
        }
    }

    private void validateAllUsersAreInTheTenant(Workspace workspace, Set<User> users) {
        boolean anyUserNotInTenantOfWorkspace = users.stream()
                .anyMatch(user -> !user.getTenant().equals(workspace.getTenant()));

        if (anyUserNotInTenantOfWorkspace) {
            throw new NotFoundException("User not found in tenant.");
        }
    }

    private void authorizeWorkspaceManipulation(User currentUser, Workspace workspaceToManipulate, Action action) {
        UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserAndWorkspace(currentUser, workspaceToManipulate);
        if (userWorkspacePermissions == null) {
            throw new AccessDeniedException("You have no access for this resource.");
        }
        boolean hasPermission = WorkspacePermissions.hasPermission(userWorkspacePermissions.getPermissionSet(), WorkspaceResource.WORKSPACE, action);
        if (!hasPermission) {
            throw new AccessDeniedException("You cannot delete this workspace.");
        }
    }

    private void setupDeletionDateAndFlag(Workspace workspaceForDelete) {
        workspaceForDelete.setStatus(DELETED);
        workspaceForDelete.setDeletionTimestamp(Calendar.getInstance().getTimeInMillis());
    }
}