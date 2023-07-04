package com.myorg;

import com.myorg.my_construct.BastionHost;
import com.myorg.my_construct.Database;
import com.myorg.my_construct.Network;
import com.myorg.my_construct.Service;
import com.myorg.util.ApplicationEnvironment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public class CdkAppStack extends Stack {


    public CdkAppStack(final Construct scope, final String id, final StackProps props) {

        super(scope, id, props);

        String appName             = (String) this.getNode().tryGetContext("applicationName");
        String envName             = (String) this.getNode().tryGetContext("environmentName");
        String az1                 = (String) this.getNode().tryGetContext("az1");
        String az2                 = (String) this.getNode().tryGetContext("az2");
        String appAmiImageId       = (String) this.getNode().tryGetContext("appAmiImageName");
        String instanceKeyPairName = (String) this.getNode().tryGetContext("instanceKeyPairName");
        String s3Arn               = (String) this.getNode().tryGetContext("s3Arn");
        String dbUser              = (String) this.getNode().tryGetContext("dbUser");

        Network network = new Network(this, "network", envName, new Network.NetworkInputParams(az1, az2));
        Network.NetworkOutputParameters networkOutParams = network.getOutputParameters();

        BastionHost bastionHost = new BastionHost(this, "bastion-host", envName,
                new BastionHost.BastionHostInputParameters(instanceKeyPairName), networkOutParams);
        BastionHost.BastionHostOutputParameters bastionHostOutParams = bastionHost.getOutputParameters();

        Service service = new Service(this, "service", new ApplicationEnvironment(appName, envName),
                new Service.ServiceInputParameters(appAmiImageId, instanceKeyPairName, s3Arn), networkOutParams);
        Service.ServiceOutputParameters serviceOutParams = service.getOutputParameters();

        Database database = new Database(this, "database", new ApplicationEnvironment(appName, envName),
                new Database.DatabaseInputParameters(dbUser, serviceOutParams.getAppSgId(),
                        bastionHostOutParams.getBhSgId()), networkOutParams);
    }
}
