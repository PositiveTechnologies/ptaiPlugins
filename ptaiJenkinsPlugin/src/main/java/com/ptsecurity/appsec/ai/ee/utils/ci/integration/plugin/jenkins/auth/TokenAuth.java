package com.ptsecurity.appsec.ai.ee.utils.ci.integration.plugin.jenkins.auth;

import com.ptsecurity.appsec.ai.ee.utils.ci.integration.jenkins.SastJob;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.jenkins.exceptions.JenkinsClientException;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.plugin.jenkins.Messages;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.plugin.jenkins.credentials.ServerCredentials;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.plugin.jenkins.credentials.ServerCredentialsImpl;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.plugin.jenkins.utils.Validator;
import hudson.Extension;
import hudson.model.Item;
import hudson.util.FormValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.URL;

@EqualsAndHashCode
public class TokenAuth extends Auth {
    @Getter
    private String userName;
    @Getter
    private String apiToken;

    @DataBoundConstructor
    public TokenAuth(final String userName,
                     final String apiToken) {
        this.userName = userName;
        this.apiToken = apiToken;
    }

    @Symbol("TokenAuth")
    @Extension
    public static class TokenAuthDescriptor extends AuthDescriptor {
        @Override
        public String getDisplayName() {
            return "Token Authentication";
        }

        public FormValidation doTestJenkinsServer(
                @AncestorInPath Item item,
                @QueryParameter("jenkinsServerUrl") final String jenkinsServerUrl,
                @QueryParameter("jenkinsJobName") final String jenkinsJobName,
                @QueryParameter("serverCredentialsId") final String serverCredentialsId,
                @QueryParameter("userName") final String userName,
                @QueryParameter("apiToken") final String apiToken) throws IOException {
            try {
                if (StringUtils.isEmpty(jenkinsServerUrl))
                    throw new JenkinsClientException(Messages.validator_emptyJenkinsHostUrl());
                if (StringUtils.isEmpty(jenkinsJobName))
                    throw new JenkinsClientException(Messages.validator_emptyJenkinsJobName());
                if (StringUtils.isEmpty(serverCredentialsId))
                    if ("https".equalsIgnoreCase(new URL(jenkinsServerUrl).getProtocol()))
                        throw new JenkinsClientException(Messages.validator_emptyPtaiCaCerts());
                if (StringUtils.isEmpty(userName))
                    throw new JenkinsClientException(Messages.validator_emptyJenkinsUserName());
                if (StringUtils.isEmpty(apiToken))
                    throw new JenkinsClientException(Messages.validator_emptyJenkinsApiToken());

                ServerCredentials serverCredentials = ServerCredentialsImpl.getCredentialsById(item, serverCredentialsId);

                SastJob jenkinsClient = new SastJob();
                jenkinsClient.setUrl(jenkinsServerUrl);
                jenkinsClient.setCaCertsPem(serverCredentials.getServerCaCertificates());
                jenkinsClient.setJobName(jenkinsJobName);
                jenkinsClient.setUserName(userName);
                jenkinsClient.setPassword(apiToken);
                jenkinsClient.init();
                return FormValidation.ok(Messages.validator_successSastJobName(jenkinsClient.testSastJob()));
            } catch (Exception e) {
                return Validator.error(e);
            }
        }
    }
}
