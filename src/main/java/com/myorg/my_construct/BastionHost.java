package com.myorg.my_construct;

import lombok.Getter;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.List;

public class BastionHost extends Construct {

    private final String        envName;
    private final SecurityGroup bhSg;

    public BastionHost(
            Construct scope, String id, String envName, BastionHostInputParameters serviceInputParameters,
            Network.NetworkOutputParameters networkOutputParameters) {

        super(scope, id);

        this.envName = envName;

        bhSg = createBhSg(networkOutputParameters.getVpc());
        Role role = createBhRole();
        Instance bhInstance =
                createBhInstance(serviceInputParameters.keyPairName, networkOutputParameters.getVpc(), bhSg, role);

        Tags.of(this).add("environment", envName);
    }

    private Instance createBhInstance(String keyPairName, IVpc vpc, ISecurityGroup sg, IRole role) {

        return Instance
                .Builder
                .create(this, "bh-instance")
                .instanceName(prefixWithEnvName("bh-instance"))
                .machineImage(MachineImage.latestAmazonLinux2())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .keyName(keyPairName)
                .vpc(vpc)
//                .vpcSubnets(SubnetSelection.builder().subnets(List.of(subnet)).build())
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .securityGroup(sg)
                .role(role)
                .build();
    }

    private SecurityGroup createBhSg(IVpc vpc) {

        SecurityGroup sg = SecurityGroup
                .Builder
                .create(this, "bh-sg")
                .securityGroupName(prefixWithEnvName("bh-sg"))
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(22));

        return sg;
    }

    private Role createBhRole() {

        return Role
                .Builder
                .create(this, "ec2-bh-role")
                .roleName("EC2BastionHostRole")
                .assumedBy(ServicePrincipal.Builder.create("ec2.amazonaws.com").build())
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore")))
                .build();
    }

    private String prefixWithEnvName(String string) {

        return envName + "-" + string;
    }

    public BastionHostOutputParameters getOutputParameters() {

        return new BastionHostOutputParameters(bhSg.getSecurityGroupId());
    }

    public static class BastionHostInputParameters {

        private final String keyPairName;

        public BastionHostInputParameters(String keyPairName) {

            this.keyPairName = keyPairName;
        }
    }

    @Getter
    public static class BastionHostOutputParameters {

        private final String bhSgId;

        public BastionHostOutputParameters(String bhSgId) {

            this.bhSgId = bhSgId;
        }
    }
}
