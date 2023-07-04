package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkAppApp {

    public static void main(final String[] args) {

        App app = new App();

        new CdkAppStack(app, "appStack",
                StackProps
                        .builder()
                        .stackName("SpringBootApplication")
                        .env(Environment.builder()
                                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                                        .region(System.getenv("CDK_DEFAULT_REGION"))
                                        .build())
                        .build());

        app.synth();
    }
}

