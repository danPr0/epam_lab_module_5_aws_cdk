package com.myorg.my_construct;

import com.myorg.util.ApplicationEnvironment;
import lombok.Getter;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class Service extends Construct {

    private final ApplicationEnvironment appEnv;
    private final SecurityGroup          appSg;

    public Service(
            Construct scope, String id, ApplicationEnvironment appEnv, ServiceInputParameters serviceInputParameters,
            Network.NetworkOutputParameters networkOutputParameters) {

        super(scope, id);

        this.appEnv = appEnv;

        appSg = createAppSg(networkOutputParameters.getVpc());
        Role appRole = createRole(serviceInputParameters.s3Arn);
        Instance appInstance =
                createAppInstance(serviceInputParameters.imageName, serviceInputParameters.keyPairName,
                        networkOutputParameters.getVpc(), appSg, appRole);

        appEnv.tag(this);
    }

    private Instance createAppInstance(
            String imageName, String keyPairName, IVpc vpc, ISecurityGroup sg,
            IRole role) {

        return Instance
                .Builder
                .create(this, "app-instance")
                .instanceName(appEnv.prefix("app-instance"))
                .machineImage(MachineImage.lookup(LookupMachineImageProps.builder().name(imageName).build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .keyName(keyPairName)
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .securityGroup(sg)
                .role(role)
                .build();
    }

    private SecurityGroup createAppSg(IVpc vpc) {

        SecurityGroup sg = SecurityGroup
                .Builder
                .create(this, "app-sg")
                .securityGroupName(appEnv.prefix("app-sg"))
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(80));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(22));

        return sg;
    }

    private Role createRole(String s3Arn) {

        return Role
                .Builder
                .create(this, "ec2-app-role")
                .roleName("EC2ApplicationRole")
                .assumedBy(ServicePrincipal.Builder.create("ec2.amazonaws.com").build())
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"),
                        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentServerPolicy"),
                        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentAdminPolicy")))
                .inlinePolicies(Map.of(
                        appEnv.prefix("s3GetObjectPolicy"),
                        PolicyDocument
                                .Builder
                                .create()
                                .statements(singletonList(
                                        PolicyStatement
                                                .Builder.create()
                                                        .effect(Effect.ALLOW)
                                                        .resources(singletonList(s3Arn + "/*"))
                                                        .actions(singletonList("s3:GetObject"))
                                                        .build()))
                                .build()))
                .build();
    }

    public ServiceOutputParameters getOutputParameters() {

        return new ServiceOutputParameters(appSg.getSecurityGroupId());
    }

    public static class ServiceInputParameters {

        private final String imageName;
        private final String keyPairName;
        private final String s3Arn;

        public ServiceInputParameters(String imageName, String keyPairName, String s3Arn) {

            this.imageName   = imageName;
            this.keyPairName = keyPairName;
            this.s3Arn       = s3Arn;
        }
    }

    @Getter
    public static class ServiceOutputParameters {

        private final String appSgId;

        public ServiceOutputParameters(String appSgId) {

            this.appSgId = appSgId;
        }
    }
}
