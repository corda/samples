package net.corda.samples.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${partyA.host}")
    private String partyAHostAndPort;

    @Value("${partyB.host}")
    private String partyBHostAndPort;

    @Value("${partyC.host}")
    private String partyCHostAndPort;

    @Bean(destroyMethod = "")  // Avoids node shutdown on rpc disconnect
    public CordaRPCOps partyAProxy(){
        CordaRPCClient partyAClient = new CordaRPCClient(NetworkHostAndPort.parse(partyAHostAndPort));
        return partyAClient.start("user1", "test").getProxy();
    }

    @Bean(destroyMethod = "")
    public CordaRPCOps partyBProxy(){
        CordaRPCClient partyBClient = new CordaRPCClient(NetworkHostAndPort.parse(partyBHostAndPort));
        return partyBClient.start("user1", "test").getProxy();
    }

    @Bean(destroyMethod = "")
    public CordaRPCOps partyCProxy(){
        CordaRPCClient partyCClient = new CordaRPCClient(NetworkHostAndPort.parse(partyCHostAndPort));
        return partyCClient.start("user1", "test").getProxy();
    }

    /**
     * Corda Jackson Support, to convert corda objects to json
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
        ObjectMapper mapper =  JacksonSupport.createDefaultMapper(partyAProxy());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        return converter;
    }
}
