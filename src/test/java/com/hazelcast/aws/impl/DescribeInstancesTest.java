/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.aws.impl;

import com.hazelcast.aws.utility.Environment;
import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class DescribeInstancesTest {

    @Test(expected = IllegalArgumentException.class)
    public void test_whenAwsConfigIsNull() throws IOException {
        new DescribeInstances(null, "endpoint");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_whenAccessKey_And_IamRole_And_IamTaskRoleEnvVar_Null_With_No_DefaultRole() throws IOException {
        Environment mockedEnv = mock(Environment.class);
        when(mockedEnv.getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME)).thenReturn(null);

        final String uri = "http://" + DescribeInstances.IAM_ROLE_ENDPOINT + "/latest/meta-data/iam/security-credentials/";

        DescribeInstances descriptor = spy(new DescribeInstances(new AwsConfig()));
        doReturn("").when(descriptor).retrieveRoleFromURI(uri);
        descriptor.checkKeysFromIamRoles(mockedEnv);
    }

    @Test
    public void test_whenAccessKey_And_IamTaskRoleEnvVar_Null_With_DefaultRole_Assigned() throws IOException {
        Environment mockedEnv = mock(Environment.class);
        when(mockedEnv.getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME)).thenReturn(null);

        final String defaultIamRoleName = "defaultIamRole";
        final String uri = "http://" + DescribeInstances.IAM_ROLE_ENDPOINT + "/latest/meta-data/iam/security-credentials/";

        final String roleUri = "http://" + DescribeInstances.IAM_ROLE_ENDPOINT + "/latest/meta-data/iam/security-credentials/"+defaultIamRoleName;
        // some dummy creds. Look real, but they aren't.
        final String accessKeyId = "ASIAJDOR231233BVE7GQ";
        final String secretAccessKey = "QU5mTd40xnAbC5Mz2T3Fy7afQVrow+/tYq5GXMf7";
        final String token = "FQoDYXdzEKX//////////wEaDN2Xh+ekVbV1KJrCqCK3A/Quuw8xCdZZbOPjzKLNc89n72z61BLt96hzlxTV6Vx1hDXLQNWRIx07hZVgmgGzzyr0DzYAcqKq7s2GUznWlaXhGHxhyo4nJUeBFbLyYPjbDAcnl84HItjy5bvtQ6fbDM7h2ZGuJrHi51KAhxWN/uEHyBKAIJd5RdXxVH4UTNxJFiqEw8GdaXDGK07186TfqSFCdlG+rhL35bN7WcJZuykIpynbeQpPeY4rJ0WJGoSJwt/RSkGwP+JRcYmv8Y7L1uSD2spJWO6etFeyyU63y0BL42MXWL38SQypxjLz+s1PozSDrV7zxsp4DQONn+adbSyAoveskD3xtDYsip1Ra0UCSYNKzmmh2XXF4fBBb6EPRixc1fnCIVDp0rfyCGO0VMuIloF5nWP9XsaRcR1mbJ7K/TuWgugduRBgyV2s1KgJuPni5cZ6ptEkPBb2b+92DjxEdQCAi6+WAdWliFiJ/P3T+qSJGLaxAeu0P0yb8E2xfCjEH6qOH3EM0KfgyJM5WJbXlYZTOZZXHaj26rlhe2k3wdL+UXf4geAzczphyOyp4QIGqaxe0xj08BKvSqngQb5X44oVR40oi7fOvwU=";

        final String someDummyIamRole =
          "        {\n" +
            "          \"Code\" : \"Success\",\n" +
            "          \"LastUpdated\" : \"2016-10-04T12:08:24Z\",\n" +
            "          \"Type\" : \"AWS-HMAC\",\n" +
            "          \"AccessKeyId\" : \""+accessKeyId+"\",\n" +
            "          \"SecretAccessKey\" : \""+secretAccessKey+"\",\n" +
            "          \"Token\" : \""+token+"\",\n" +
            "          \"Expiration\" : \"2016-10-04T18:19:39Z\"\n" +
            "        }\n";


        // test when <iam-role>DEFAULT</iam-role>
        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setIamRole("DEFAULT");

        DescribeInstances descriptor = spy(new DescribeInstances(awsConfig));
        doReturn(defaultIamRoleName).when(descriptor).retrieveRoleFromURI(uri);
        doReturn(someDummyIamRole).when(descriptor).retrieveRoleFromURI(roleUri);
        descriptor.checkKeysFromIamRoles(mockedEnv);

        Assert.assertEquals("Could not parse access key from IAM role", accessKeyId, awsConfig.getAccessKey());
        Assert.assertEquals("Could not parse secret key from IAM role", secretAccessKey, awsConfig.getSecretKey());

        // test when <iam-role></iam-role>
        awsConfig = new AwsConfig();
        awsConfig.setIamRole("");

        descriptor = spy(new DescribeInstances(awsConfig));
        doReturn(defaultIamRoleName).when(descriptor).retrieveRoleFromURI(uri);
        doReturn(someDummyIamRole).when(descriptor).retrieveRoleFromURI(roleUri);
        descriptor.checkKeysFromIamRoles(mockedEnv);

        Assert.assertEquals("Could not parse access key from IAM role", accessKeyId, awsConfig.getAccessKey());
        Assert.assertEquals("Could not parse secret key from IAM role", secretAccessKey, awsConfig.getSecretKey());

        // test when no <iam-role></iam-role> defined, BUT default IAM role has been assigned
        awsConfig = new AwsConfig();

        descriptor = spy(new DescribeInstances(awsConfig));
        doReturn(defaultIamRoleName).when(descriptor).retrieveRoleFromURI(uri);
        doReturn(someDummyIamRole).when(descriptor).retrieveRoleFromURI(roleUri);
        descriptor.checkKeysFromIamRoles(mockedEnv);

        Assert.assertEquals("Could not parse access key from IAM role", accessKeyId, awsConfig.getAccessKey());
        Assert.assertEquals("Could not parse secret key from IAM role", secretAccessKey, awsConfig.getSecretKey());

    }


    @Test
    public void test_whenAccessKeyExistsInConfig() throws IOException {
        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setAccessKey("accesskey");
        awsConfig.setSecretKey("secretkey");
        new DescribeInstances(awsConfig, "endpoint");
    }

    @Test(expected = InvalidConfigurationException.class)
    public void test_whenBoth_AccessKey_And_IamRole_Are_Defined() throws IOException {
        Environment mockedEnv = mock(Environment.class);
        when(mockedEnv.getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME)).thenReturn(null);

        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setAccessKey("accesskey");
        awsConfig.setSecretKey("secretkey");
        awsConfig.setIamRole("someRole");

        DescribeInstances descriptor = new DescribeInstances(awsConfig);
        descriptor.checkKeysFromIamRoles(mockedEnv);
    }


    @Test
    public void test_whenIamRoleExistsInConfig() throws IOException {
        final String someRole = "someRole";
        final String uri = "http://" + DescribeInstances.IAM_ROLE_ENDPOINT + "/latest/meta-data/iam/security-credentials/"+someRole;

        // some dummy creds. Look real, but they aren't.
        final String accessKeyId = "ASIAJDOR231233BVE7GQ";
        final String secretAccessKey = "QU5mTd40xnAbC5Mz2T3Fy7afQVrow+/tYq5GXMf7";
        final String token = "FQoDYXdzEKX//////////wEaDN2Xh+ekVbV1KJrCqCK3A/Quuw8xCdZZbOPjzKLNc89n72z61BLt96hzlxTV6Vx1hDXLQNWRIx07hZVgmgGzzyr0DzYAcqKq7s2GUznWlaXhGHxhyo4nJUeBFbLyYPjbDAcnl84HItjy5bvtQ6fbDM7h2ZGuJrHi51KAhxWN/uEHyBKAIJd5RdXxVH4UTNxJFiqEw8GdaXDGK07186TfqSFCdlG+rhL35bN7WcJZuykIpynbeQpPeY4rJ0WJGoSJwt/RSkGwP+JRcYmv8Y7L1uSD2spJWO6etFeyyU63y0BL42MXWL38SQypxjLz+s1PozSDrV7zxsp4DQONn+adbSyAoveskD3xtDYsip1Ra0UCSYNKzmmh2XXF4fBBb6EPRixc1fnCIVDp0rfyCGO0VMuIloF5nWP9XsaRcR1mbJ7K/TuWgugduRBgyV2s1KgJuPni5cZ6ptEkPBb2b+92DjxEdQCAi6+WAdWliFiJ/P3T+qSJGLaxAeu0P0yb8E2xfCjEH6qOH3EM0KfgyJM5WJbXlYZTOZZXHaj26rlhe2k3wdL+UXf4geAzczphyOyp4QIGqaxe0xj08BKvSqngQb5X44oVR40oi7fOvwU=";

        final String someDummyIamRole =
          "        {\n" +
            "          \"Code\" : \"Success\",\n" +
            "          \"LastUpdated\" : \"2016-10-04T12:08:24Z\",\n" +
            "          \"Type\" : \"AWS-HMAC\",\n" +
            "          \"AccessKeyId\" : \""+accessKeyId+"\",\n" +
            "          \"SecretAccessKey\" : \""+secretAccessKey+"\",\n" +
            "          \"Token\" : \""+token+"\",\n" +
            "          \"Expiration\" : \"2016-10-04T18:19:39Z\"\n" +
            "        }\n";


        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setIamRole(someRole);

        DescribeInstances descriptor = spy(new DescribeInstances(awsConfig));
        doReturn(someDummyIamRole).when(descriptor).retrieveRoleFromURI(uri);
        descriptor.checkKeysFromIamRoles(null);

        Assert.assertEquals("Could not parse access key from IAM role", accessKeyId, awsConfig.getAccessKey());
        Assert.assertEquals("Could not parse secret key from IAM role", secretAccessKey, awsConfig.getSecretKey());

    }

    @Test
    public void test_when_Empty_IamRole_And_DefaultIamRole_But_IamTaskRoleEnvVar_Exists() throws IOException {

        final String accessKeyId = "ASIAJZ6Y3MXO7SRJ1234";
        final String secretAccessKey = "p00VnhQ//HeT7W7V123f7BgYZaBlPZTxj9mcvSlc";
        final String token = "FQoDYXdzEKX//////////wEaDDS1irrM7Wkt1VxNUyKoAxdXWEDQJUXpIGmBG4qCCiNLOXkF5mak8ZDVqS2PV+X7nsRF9C4mZwqMkGfRmxpZzGc+QTRfbncdZzEOeHcBea38mM7kJUJyNagWfNpwgzimgJzLqn5tNirs7+MXVw5rfblWCjngzjrovlsl6+q0K9LZM0W5OTRSKmEZQnJFjZh9w+BZHo5pair1ZqrxhfOcW6UaMpfOfRH/VI1n3u+De7YCqdq5jhmaDzWxewxccfH/BI2SRHaC1OEq0L3kMhwj1JrLjrOTJn4nBwjGZAlODFhoMec1cUW0GdIJN6+KZDbt8TuKlqDutKMDe1CNIH/697J0lLPMC8tgbgu3MrLSVxtQkMPdDMzJWNPXNQhQa+Nvw5w7gV7+27s9oat+dJBp3lLmTe4PZ810IQGa2NLZHKrv7kqGncLu5mURj+UVZHlueyYyPBWhVdHn4tCJ/cX2RaeqiVTbMILrduZePw7wTS8+19RnnPxA9wp45OZE5otSvNegJ9XhEJiU7RyJPhdSezMVoGsnzvgbCnBUAzHAe4ZuQZo7iIBWNXkcKBsCAU0MY8ym6NVn5VQohKrOvwU=";

        // Note the below role is different from the regular IAM role, in that it doesn't contain new lines.
        final String someDummyIamTaskRole =
          "{" +
            "  \"RoleArn\":\"arn:aws:iam::123456789012:role/hazelcastIamTaskRole\"," +
            "  \"AccessKeyId\":\""+accessKeyId+"\"," +
            "  \"SecretAccessKey\":\""+secretAccessKey+"\"," +
            "  \"Token\":\""+token+"\"," +
            "  \"Expiration\":\"2016-10-04T17:39:48Z\"" +
            "  }";


        final String ecsEnvVarCredsUri = "someURL";
        final String uri = "http://" + DescribeInstances.IAM_TASK_ROLE_ENDPOINT + ecsEnvVarCredsUri;
        final String defaultRoleUri = "http://" + DescribeInstances.IAM_ROLE_ENDPOINT + "/latest/meta-data/iam/security-credentials/";

        Environment mockedEnv = mock(Environment.class);
        when(mockedEnv.getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME)).thenReturn(ecsEnvVarCredsUri);

        AwsConfig awsConfig = new AwsConfig();

        // test when default role is null
        DescribeInstances descriptor = spy(new DescribeInstances(awsConfig));
        doReturn(someDummyIamTaskRole).when(descriptor).retrieveRoleFromURI(uri);
        doReturn("").when(descriptor).retrieveRoleFromURI(defaultRoleUri);

        descriptor.checkKeysFromIamRoles(mockedEnv);

        Assert.assertEquals("Could not parse access key from IAM task role", accessKeyId, awsConfig.getAccessKey());
        Assert.assertEquals("Could not parse secret key from IAM task role", secretAccessKey, awsConfig.getSecretKey());

    }
}
