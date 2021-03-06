package com.sequenceiq.environment.environment.v1;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentDeletionService;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStartService;
import com.sequenceiq.environment.environment.service.EnvironmentStopService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;

@Controller
@InternalReady
@Transactional(TxType.NEVER)
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class EnvironmentController implements EnvironmentEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final EnvironmentDeletionService environmentDeletionService;

    private final EnvironmentModificationService environmentModificationService;

    private final EnvironmentStartService environmentStartService;

    private final EnvironmentStopService environmentStopService;

    private final CredentialService credentialService;

    public EnvironmentController(
            EnvironmentApiConverter environmentApiConverter,
            EnvironmentResponseConverter environmentResponseConverter,
            EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService,
            EnvironmentDeletionService environmentDeletionService,
            EnvironmentModificationService environmentModificationService,
            EnvironmentStartService environmentStartService,
            EnvironmentStopService environmentStopService,
            CredentialService credentialService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentResponseConverter = environmentResponseConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentDeletionService = environmentDeletionService;
        this.environmentModificationService = environmentModificationService;
        this.environmentStartService = environmentStartService;
        this.environmentStopService = environmentStopService;
        this.credentialService = credentialService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE)
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        EnvironmentDto envDto = environmentCreationService.create(environmentCreationDto);
        return environmentResponseConverter.dtoToDetailedResponse(envDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DetailedEnvironmentResponse getByName(String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentResponseConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public EnvironmentCrnResponse getCrnByName(String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String crn = environmentService.getCrnByNameAndAccountId(environmentName, accountId);
        return environmentApiConverter.crnResponse(environmentName, crn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DetailedEnvironmentResponse getByCrn(@TenantAwareParam String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentResponseConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SimpleEnvironmentResponse deleteByName(String environmentName, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentDeletionService.deleteByNameAndAccountId(environmentName, accountId, actualUserCrn, forced);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SimpleEnvironmentResponse deleteByCrn(@TenantAwareParam String crn, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentDeletionService.deleteByCrnAndAccountId(crn, accountId, actualUserCrn, forced);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SimpleEnvironmentResponses deleteMultipleByNames(Set<String> environmentNames, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentDeletionService.deleteMultipleByNames(environmentNames, accountId, actualUserCrn, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentResponseConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public SimpleEnvironmentResponses deleteMultipleByCrns(Set<String> crns, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentDeletionService.deleteMultipleByCrns(crns, accountId, actualUserCrn, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentResponseConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DetailedEnvironmentResponse editByName(String environmentName, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByName(environmentName, editDto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DetailedEnvironmentResponse editByCrn(String crn, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByCrn(crn, editDto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public SimpleEnvironmentResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        List<EnvironmentDto> environmentDtos = environmentService.listByAccountId(accountId);
        List<SimpleEnvironmentResponse> responses = environmentDtos.stream().map(environmentResponseConverter::dtoToSimpleResponse)
                .collect(Collectors.toList());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DetailedEnvironmentResponse changeCredentialByEnvironmentName(String environmentName, @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentChangeCredentialDto dto = environmentApiConverter.convertEnvironmentChangeCredentialDto(request);
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentName(accountId, environmentName, dto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentName(String name, @Valid FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentFeatures features = environmentApiConverter.convertToEnvironmentTelemetryFeatures(request);
        EnvironmentDto result = environmentModificationService.changeTelemetryFeaturesByEnvironmentName(accountId, name, features);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DetailedEnvironmentResponse changeCredentialByEnvironmentCrn(String crn, @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentCrn(accountId, crn,
                environmentApiConverter.convertEnvironmentChangeCredentialDto(request));
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentCrn(String crn, @Valid FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentFeatures features = environmentApiConverter.convertToEnvironmentTelemetryFeatures(request);
        EnvironmentDto result = environmentModificationService.changeTelemetryFeaturesByEnvironmentCrn(accountId, crn, features);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void postStartByName(String name) {
        environmentStartService.startByName(name);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void postStartByCrn(@TenantAwareParam String crn) {
        environmentStartService.startByCrn(crn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void postStopByName(String name) {
        environmentStopService.stopByName(name);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void postStopByCrn(@TenantAwareParam String crn) {
        environmentStopService.stopByCrn(crn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public Object getCreateEnvironmentForCliByName(String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentService.getCreateEnvironmentForCli(environmentDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public Object getCreateEnvironmentForCliByCrn(@TenantAwareParam String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentService.getCreateEnvironmentForCli(environmentDto);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public Object getCreateEnvironmentForCli(EnvironmentRequest environmentRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(environmentRequest.getCredentialName(), accountId);
        return environmentService.getCreateEnvironmentForCli(environmentRequest, credential.getCloudPlatform());
    }
}
