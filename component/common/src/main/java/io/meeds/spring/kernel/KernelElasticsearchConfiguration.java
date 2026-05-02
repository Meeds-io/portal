/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.spring.kernel;

import static org.springframework.data.elasticsearch.client.elc.ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint;
import org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import lombok.Setter;

@Configuration
public class KernelElasticsearchConfiguration extends ElasticsearchConfiguration {

  @Value("${exo.es.search.server.username:}")
  private String                        esUsername;

  @Value("${exo.es.search.server.password:}")
  private String                        esPassword;

  @Value("${exo.es.search.server.url:http://127.0.0.1:9200}")
  private String                        esUrl;

  @Value("${exo.es.search.socketTimeout:20}")
  private int                           socketTimeout;

  @Value("${exo.es.search.connectTimeout:20}")
  private int                           connectionTimeout;

  @Value("${exo.es.search.connectionRetry:500}")
  private int                           connectionRetry;

  @Value("${exo.es.search.http.connections.max:100}")
  private int                           maxPoolConnections;

  @Setter
  private static ClientConfiguration    clientConfiguration;

  @Setter
  private static RestClient             elasticsearchRestClient;

  @Setter
  private static ElasticsearchTransport elasticsearchTransport;

  @Setter
  private static ElasticsearchClient    elasticsearchClient;

  @Setter
  private static JsonpMapper            jsonpMapper;

  @Override
  public ClientConfiguration clientConfiguration() {
    if (clientConfiguration == null) {
      String hostAndPort = esUrl.split("//")[1];
      ClientConfigurationBuilderWithRequiredEndpoint builder = ClientConfiguration.builder();
      MaybeSecureClientConfigurationBuilder connectionBuilder = builder.connectedTo(hostAndPort);
      if (esUrl.contains("https://")) {
        connectionBuilder.usingSsl();
      }
      if (StringUtils.isNotBlank(esPassword) && StringUtils.isNotBlank(esUsername)) {
        connectionBuilder.withBasicAuth(esUsername, esPassword);
      }
      setClientConfiguration(connectionBuilder.withConnectTimeout(Duration.ofSeconds(connectionTimeout))
                                              .withSocketTimeout(Duration.ofSeconds(socketTimeout))
                                              .withClientConfigurer(from(this::setMaxThreads))
                                              .build());
    }
    return clientConfiguration;
  }

  @Override
  public RestClient elasticsearchRestClient(ClientConfiguration clientConfiguration) {
    if (elasticsearchRestClient == null) {
      setElasticsearchRestClient(super.elasticsearchRestClient(clientConfiguration));
    }
    return elasticsearchRestClient;
  }

  @Override
  public JsonpMapper jsonpMapper() {
    if (jsonpMapper == null) {
      setJsonpMapper(super.jsonpMapper());
    }
    return jsonpMapper;
  }

  @Override
  public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {
    if (elasticsearchTransport == null) {
      setElasticsearchTransport(super.elasticsearchTransport(restClient, jsonpMapper));
    }
    return elasticsearchTransport;
  }

  @Override
  public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
    if (elasticsearchClient == null) {
      setElasticsearchClient(super.elasticsearchClient(transport));
    }
    return elasticsearchClient;
  }

  @Override
  public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
                                                         ElasticsearchClient elasticsearchClient) {
    ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
    elasticsearchTemplate.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
    return elasticsearchTemplate;
  }

  private HttpAsyncClientBuilder setMaxThreads(HttpAsyncClientBuilder httpClientBuilder) {
    int ioThreadsCount = Math.min(Runtime.getRuntime().availableProcessors(), 4);
    return httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom()
                                                                      .setIoThreadCount(ioThreadsCount)
                                                                      .build())
                            .setMaxConnTotal(maxPoolConnections)
                            .setMaxConnPerRoute(maxPoolConnections);
  }

}
