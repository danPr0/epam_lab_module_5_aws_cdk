package com.myorg.my_construct;

import com.myorg.util.ApplicationEnvironment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.util.List;

public class Database extends Construct {

    private final ApplicationEnvironment appEnv;

    public Database(
            Construct scope, String id, ApplicationEnvironment appEnv,
            DatabaseInputParameters databaseInputParameters, Network.NetworkOutputParameters networkOutputParameters) {

        super(scope, id);

        this.appEnv = appEnv;

        SecurityGroup dbSg = createDbSg(networkOutputParameters.getVpc(), databaseInputParameters.appSgId,
                databaseInputParameters.bhSgId);
        Secret      dbSecret      = createDbSecret(databaseInputParameters.username);
        SubnetGroup dbSubnetGroup = createDbSubnetGroup(networkOutputParameters.getVpc());
        DatabaseInstance dbInstance = createDbInstance(dbSecret, networkOutputParameters.getVpc(), dbSubnetGroup, dbSg);

        appEnv.tag(this);
    }

    private DatabaseInstance createDbInstance(
            Secret dbSecret, IVpc vpc, ISubnetGroup subnetGroup, ISecurityGroup dbSg) {

        return DatabaseInstance
                .Builder
                .create(this, "ads")
                .engine(DatabaseInstanceEngine.mysql(
                        MySqlInstanceEngineProps.builder().version(MysqlEngineVersion.VER_8_0_32).build()))
                .instanceIdentifier(appEnv.prefix("database"))
                .credentials(Credentials.fromSecret(dbSecret))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .storageType(StorageType.GP2)
                .allocatedStorage(20)
                .maxAllocatedStorage(20)
                .vpc(vpc)
                .subnetGroup(subnetGroup)
                .publiclyAccessible(false)
                .securityGroups(List.of(dbSg))
                .iamAuthentication(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private SecurityGroup createDbSg(IVpc vpc, String appSgId, String bhSgId) {

        SecurityGroup sg = SecurityGroup
                .Builder
                .create(this, "db-sg")
                .securityGroupName(appEnv.prefix("db-sg"))
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        sg.addIngressRule(Peer.securityGroupId(appSgId), Port.tcp(3306));
        sg.addIngressRule(Peer.securityGroupId(bhSgId), Port.tcp(3306));

        return sg;
    }

    private Secret createDbSecret(String username) {

        return Secret.Builder.create(this, "databaseSecret")
                             .secretName(appEnv.prefix("DatabaseSecret"))
                             .description("Credentials to the RDS instance")
                             .generateSecretString(SecretStringGenerator
                                     .builder()
                                     .secretStringTemplate(String.format("{\"username\": \"%s\"}", username))
                                     .generateStringKey("password")
                                     .passwordLength(8)
                                     .excludeCharacters("@/\\\" ")
                                     .build())
                             .build();
    }

    private SubnetGroup createDbSubnetGroup(IVpc vpc) {

        return SubnetGroup
                .Builder
                .create(this, "dbSubnetGroup")
                .subnetGroupName(appEnv.prefix("dbSubnetGroup"))
                .description("Subnet group for the RDS instance")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
                .build();
    }

    public static class DatabaseInputParameters {

        private final String username;
        private final String appSgId;
        private final String bhSgId;

        public DatabaseInputParameters(String username, String appSgId, String bhSgId) {

            this.username = username;
            this.appSgId  = appSgId;
            this.bhSgId   = bhSgId;
        }
    }
}
