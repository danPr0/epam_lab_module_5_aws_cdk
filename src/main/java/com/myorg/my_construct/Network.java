package com.myorg.my_construct;

import lombok.Getter;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

import java.util.List;
import java.util.stream.Collectors;

public class Network extends Construct {

    private final String envName;
    private final IVpc   vpc;

    public Network(Construct scope, String id, String envName, NetworkInputParams inputParams) {

        super(scope, id);

        this.envName = envName;

        vpc = createVpc(inputParams.az1, inputParams.az2);

        configureNacls(vpc);

        Tags.of(this).add("environment", envName);
    }

    private Vpc createVpc(String az1, String az2) {

        SubnetConfiguration publicSubnet1 = SubnetConfiguration
                .builder()
                .name(prefixWithEnvName("subnet-public"))
                .subnetType(SubnetType.PUBLIC)
                .cidrMask(20)
                .build();

        SubnetConfiguration privateSubnet1 = SubnetConfiguration
                .builder()
                .name(prefixWithEnvName("subnet-private"))
                .subnetType(SubnetType.PRIVATE_ISOLATED)
                .cidrMask(20)
                .build();

        return Vpc
                .Builder
                .create(this, "vpc")
                .vpcName(prefixWithEnvName("vpc"))
                .ipAddresses(IpAddresses.cidr("10.0.0.0/16"))
//                .availabilityZones(List.of(az1, az2))
                .maxAzs(2)
                .subnetConfiguration(List.of(publicSubnet1, privateSubnet1))
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .build();
    }

    private void configureNacls(IVpc vpc) {

        NetworkAcl publicNacl = NetworkAcl
                .Builder
                .create(this, "public-acl")
                .networkAclName(prefixWithEnvName("public-acl"))
                .vpc(vpc)
                .subnetSelection(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .build();
        publicNacl.addEntry("public-acl-inbound-rule",
                CommonNetworkAclEntryOptions.builder()
                                            .direction(TrafficDirection.INGRESS)
                                            .ruleNumber(100)
                                            .traffic(AclTraffic.allTraffic())
                                            .cidr(AclCidr.anyIpv4())
                                            .ruleAction(Action.ALLOW)
                                            .build());
        publicNacl.addEntry("public-acl-outbound-rule",
                CommonNetworkAclEntryOptions.builder()
                                            .direction(TrafficDirection.EGRESS)
                                            .ruleNumber(100)
                                            .traffic(AclTraffic.allTraffic())
                                            .cidr(AclCidr.anyIpv4())
                                            .ruleAction(Action.ALLOW)
                                            .build());

        NetworkAcl privateNacl = NetworkAcl
                .Builder
                .create(this, "private-acl")
                .networkAclName(prefixWithEnvName("private-acl"))
                .vpc(vpc)
                .subnetSelection(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
                .build();
        privateNacl.addEntry("private-acl-inbound-rule",
                CommonNetworkAclEntryOptions.builder()
                                            .direction(TrafficDirection.INGRESS)
                                            .ruleNumber(100)
                                            .traffic(AclTraffic.allTraffic())
                                            .cidr(AclCidr.ipv4("10.0.0.0/16"))
                                            .ruleAction(Action.ALLOW)
                                            .build());
        privateNacl.addEntry("private-acl-outbound-rule",
                CommonNetworkAclEntryOptions.builder()
                                            .direction(TrafficDirection.EGRESS)
                                            .ruleNumber(100)
                                            .traffic(AclTraffic.allTraffic())
                                            .cidr(AclCidr.ipv4("10.0.0.0/16"))
                                            .ruleAction(Action.ALLOW)
                                            .build());
    }

    private String prefixWithEnvName(String string) {

        return envName + "-" + string;
    }

    public NetworkOutputParameters getOutputParameters() {

        return new NetworkOutputParameters(vpc, vpc.getVpcId(),
                vpc.getIsolatedSubnets().stream().map(ISubnet::getSubnetId).collect(Collectors.toList()),
                vpc.getPublicSubnets().stream().map(ISubnet::getSubnetId).collect(Collectors.toList()),
                vpc.getAvailabilityZones());
    }

    public static class NetworkInputParams {

        private final String az1, az2;

        public NetworkInputParams(String az1, String az2) {

            this.az1 = az1;
            this.az2 = az2;
        }
    }

    @Getter
    public static class NetworkOutputParameters {

        private final IVpc         vpc;
        private final String       vpcId;
        private final List<String> publicSubnets;
        private final List<String> privateSubnets;
        private final List<String> availabilityZones;

        public NetworkOutputParameters(
                IVpc vpc, String vpcId, List<String> privateSubnets, List<String> publicSubnets,
                List<String> availabilityZones) {

            this.vpc               = vpc;
            this.vpcId             = vpcId;
            this.privateSubnets    = privateSubnets;
            this.publicSubnets     = publicSubnets;
            this.availabilityZones = availabilityZones;
        }
    }
}
