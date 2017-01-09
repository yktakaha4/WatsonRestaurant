package general;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.camel.component.http4.PreemptiveAuthInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.retrieve_and_rank.v1.RetrieveAndRank;

public class APIClient {
  public static Map<String, Object> getGuruNaviResponse(String path, Map<String, Object> parameters)
      throws IOException {
    final String TARGET = "http://api.gnavi.co.jp";

    WebTarget webTarget = ClientBuilder.newClient().target(TARGET).path(path);

    if (parameters != null) {
      for (Entry<String, Object> parameter : parameters.entrySet()) {
        webTarget = webTarget.queryParam(parameter.getKey(), parameter.getValue());
      }
    }

    Common.logging(TARGET, path, parameters);
    String responseText = webTarget.request().get(String.class);

    return new ObjectMapper().readValue(responseText, new TypeReference<Map<String, Object>>() {
    });
  }

  public static Map<String, Object> getRaRSelectResponse(Map<String, Object> parameters)
      throws SolrServerException, IOException, NoSuchAlgorithmException, KeyManagementException {
    final String USERNAME = Common.prop("watson_rar_username");
    final String PASSWORD = Common.prop("watson_rar_password");
    final String CLUSTERID = Common.prop("watson_rar_clusterid");
    final String COLLECTIONNAME = Common.prop("watson_rar_collectionname");
    final String SOLR_URL = new RetrieveAndRank().getSolrUrl(CLUSTERID);

    URI uri = URI.create(SOLR_URL);
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
        new UsernamePasswordCredentials(USERNAME, PASSWORD));

    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null,
        new X509TrustManager[] { new LooseTrustManager() },
        new SecureRandom());

    HttpClient httpClient = HttpClientBuilder.create()
        .setMaxConnTotal(128)
        .setMaxConnPerRoute(32)
        .setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT).setRedirectsEnabled(true).build())
        .setDefaultCredentialsProvider(credentialsProvider)
        .addInterceptorFirst(new PreemptiveAuthInterceptor())
        .setSslcontext(sslContext)
        .build();

    HttpSolrClient httpSolrClient = new HttpSolrClient.Builder()
        .withBaseSolrUrl(SOLR_URL)
        .withHttpClient(httpClient)
        .build();

    SolrQuery solrQuery = new SolrQuery();
    for (String key : parameters.keySet()) {
      solrQuery.add(key, parameters.get(key).toString());
    }

    QueryRequest queryRequest = new QueryRequest(solrQuery);
    queryRequest.setResponseParser(new NoOpResponseParser("json"));

    String responseText = httpSolrClient.request(queryRequest, COLLECTIONNAME).get("response").toString();
    return new ObjectMapper().readValue(responseText, new TypeReference<Map<String, Object>>() {
    });
  }
}
