package com.czu.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@SpringBootTest
public class CloudProductApplicationTest {

    @Autowired
    DiscoveryClient discoveryClient;

    @Test
    void discoveryClientTest() {
        for (String service : discoveryClient.getServices()) {
            System.out.println(service);
            for (ServiceInstance instance : discoveryClient.getInstances(service)) {
                System.out.println(instance.getHost() + ":" + instance.getPort());
            }
        }
    }
}
