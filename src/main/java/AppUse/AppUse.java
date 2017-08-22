package AppUse;

import com.github.opendevl.JFlat;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

@RestController
public class AppUse {
    private static Logger logger = LoggerFactory.getLogger(AppUse.class);

    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    //This sets up an end point to induce the routine.  Scheduler will call this end point
    @RequestMapping("/usereport")
    public String index() throws Exception {
        //Let's do some logging to let us know when a call was made
        logger.info("Received a request at /usereport");

        //Let's get Environmental variables

        String endpoint = System.getenv("CF_ENDPOINT");
        String trunkuri = System.getenv("CF_APIHOST");
        String cfapi = "api.sys.gucci.gcp.pcf-metrics.com";
        Boolean skip_ssl = Boolean.valueOf(System.getenv("CF_SKIPSSL"));

        //Using the context and token providers to get a token from CAPI with a username and pwd
        DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                .apiHost(cfapi)
                .skipSslValidation(skip_ssl)
                .build();

        String pwd = System.getenv("CF_PASSWORD");
        String user = System.getenv("CF_USERNAME");

        PasswordGrantTokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                .password(pwd)
                .username(user)
                .build();

        //get a new token
        tokenProvider.invalidate(connectionContext);
        Mono<String> tokenMono = tokenProvider.getToken(connectionContext);

        // Put the token in a string Note: the string already contains the bearer prepend
        String OToken = tokenMono.block();

        //construct the url
        String targeturi = "Https://" + trunkuri + endpoint;

        //String targeturi = "https://app-usage.sys.gucci.gcp.pcf-metrics.com/system_report/app_usages";
        //For troubleshooting
        logger.info(targeturi);

        HttpHeaders headers = new HttpHeaders();
        //Add the auth token to the header of the call
        headers.set("Authorization", OToken);

        //The actual call using postForObject
        String appuseevents = restTemplate.postForObject(targeturi, headers, String.class);

        //More logging - let's us know that the call to CAPI made it.
        logger.info("Call to " + targeturi + " complete");

        //This allows you to test by just feeding it the JSON output file included in the project
        // String appuseevents = new String(Files.readAllBytes(Paths.get("/app/BOOT-INF/classes/AppUse/output.json")));
        //Next we convert the JSON output from the CAPI call to CSV
        JFlat flatMe = new JFlat(appuseevents);

        logger.info("converted output to csv");

        //Lastly we persist the data to file.
        flatMe.json2Sheet().write2csv("/app/BOOT-INF/classes/AppUse/report.csv");

        return "Report Generated";
    }
}