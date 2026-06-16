package ads.uninassau.brjobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrjobsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrjobsApplication.class, args);
	}

}
