package org.korhan.monithor.check;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.korhan.monithor.check.DataExtractor;
import org.korhan.monithor.check.HttpChecker;
import org.korhan.monithor.data.model.Job;
import org.korhan.monithor.data.model.JobResult;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpCheckerTest {

  private HttpChecker testee;
  private DataExtractor extractor = new DataExtractor();

  @Mock
  private HttpClient client;
  @Mock
  private HttpEntity httpEntity;
  @Mock
  private HttpResponse httpResponse;


  @Before
  public void setup() throws IOException {
    this.testee = new HttpChecker(client, extractor);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("YfooU 12 x13 ver = 1.20<br> a".getBytes(StandardCharsets.UTF_8)));
    when(client.execute(any(HttpGet.class))).thenReturn(httpResponse);
  }

  @Test
  public void testCheckSimpleString() {
    JobResult result = testee.check(newJob("foo"));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getError()).isNull();
  }

  @Test
  public void testCheckRegexString() {
    JobResult result = testee.check(newJob("[a-z]{1}[o-o]{2}"));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getError()).isNull();
  }

  @Test
  public void testCheckNonMatch() {
    JobResult result = testee.check(newJob("/d{2}"));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getError()).isEqualTo("success match failed");
  }

  @Test
  public void testCheckIOException() throws IOException {
    when(client.execute(any(HttpGet.class))).thenThrow(new IOException("boom"));
    JobResult result = testee.check(newJob("foo"));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getError()).contains("boom");
  }

  @Test
  public void testCheckDeploymentWrong() throws IOException {
    String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(("1701.0.13-SNAPSHOT " + now + " foo").getBytes(StandardCharsets.UTF_8)));
    JobResult result = testee.check(newJobCheckDeployment("foo"));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getBuildTimestamp()).isEqualTo(now);
    assertThat(result.getVersion()).isEqualTo("1701.0.13-SNAPSHOT");
    assertThat(result.getError()).isNull();
  }

  @Test
  public void testCheckDeploymentWrongBuildTimestamp() throws IOException {
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("1701.0.13-SNAPSHOT 2017-01-06 00:37:45 foo".getBytes(StandardCharsets.UTF_8)));
    JobResult result = testee.check(newJobCheckDeployment("foo"));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getBuildTimestamp()).isEqualTo("2017-01-06 00:37:45");
    assertThat(result.getVersion()).isEqualTo("1701.0.13-SNAPSHOT");
    assertThat(result.getError()).isEqualTo("deployment version check failed");
  }

  @Test
  public void testCheckDeploymentMissingVersion() throws IOException {
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("2017-01-06 00:37:45 foo".getBytes(StandardCharsets.UTF_8)));
    JobResult result = testee.check(newJobCheckDeployment("foo"));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getVersion()).isNull();
    assertThat(result.getBuildTimestamp()).isEqualTo("2017-01-06 00:37:45");
    assertThat(result.getError()).isEqualTo("deployment version check failed");
  }

  @Test
  public void testCheckDeploymentMissingBuildtimestamp() throws IOException {
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("1701.0.13 foo".getBytes(StandardCharsets.UTF_8)));
    JobResult result = testee.check(newJobCheckDeployment("foo"));
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getBuildTimestamp()).isNull();
    assertThat(result.getVersion()).isEqualTo("1701.0.13");
    assertThat(result.getError()).isEqualTo("deployment version check failed");
  }

  private Job newJob(String successMatch) {
    Job job = new Job();
    job.setSuccessMatch(successMatch);
    job.setUrl("testing");
    return job;
  }

  private Job newJobCheckDeployment(String successMatch) {
    Job job = newJob(successMatch);
    job.setCheckDeployment(true);
    return job;
  }
}
