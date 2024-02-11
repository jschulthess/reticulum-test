package io.reticulum.examples;

import io.reticulum.Reticulum;
import io.reticulum.Transport;
import io.reticulum.destination.Destination;
import io.reticulum.destination.DestinationType;
import io.reticulum.destination.Direction;
import io.reticulum.destination.ProofStrategy;
import io.reticulum.identity.Identity;
import io.reticulum.transport.AnnounceHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
public class AnnounceApp {
    private static final String APP_NAME = "java_example";
    Transport transport;
    Destination destination;
    //private static Logger log = LogManager.getLogger(AnnounceApp.class);
    static final  String defaultConfigPath = new String(".reticulum"); // if empty will look in Reticulums default paths
    //static final String CONFIG_FILE_NAME = new String("config.yml");
    static byte[] appData1 = "example".getBytes();

    public static void main(String[] args) throws IOException {
        //var configPath = Path.of("src/main/resources/reticulum_default_config.yml").toAbsolutePath().toString();
        //var reticulum = new Reticulum(configPath);
        //initConfig(defaultConfigPath);
        //var configDir1 = new File(".reticulum");
        //if (!configDir1.exists()) {
        //    configDir1.mkdir();
        //}
        //var configPath = Path.of(configDir1.getAbsolutePath());
        //Path configFile = configPath.resolve(CONFIG_FILE_NAME);
        //
        //if (Files.notExists(configFile)) {
        //    var defaultConfig = this.getClass().getClassLoader().getResourceAsStream("reticulum_default_config.yml");
        //    Files.copy(defaultConfig, configFile, StandardCopyOption.REPLACE_EXISTING);
        //}
        var reticulum = new Reticulum(".reticulum");
        var identity = new Identity();
        var destination = new Destination(
                identity,
                Direction.IN,
                DestinationType.SINGLE,
                APP_NAME
        );
        destination.setProofStrategy(ProofStrategy.PROVE_ALL);
        destination.announce(appData1);

        var program = new AnnounceApp(Transport.getInstance());
        program.destination = destination;
        program.announceLoop();
        log.info(destination.getHexHash());
    }

    AnnounceApp(Transport transport) {
        this.transport = transport;
        this.transport.registerAnnounceHandler(new AnnounceHandler() {
            @Override
            public String getAspectFilter() {
                log.info("getAspectFilter called.");
                //return APP_NAME;
                return null;
            }

            @Override
            public void receivedAnnounce(byte[] destinationHash, Identity announcedIdentity, byte[] appData) {
                log.info("Received an announce from {}", Hex.encodeHexString(destinationHash));

                if (appData != null) {
                    log.info("The announce contained the following app data: {}", new String(appData));
                }
            }
        });
        log.debug("announce handlers: {}", this.transport.getAnnounceHandlers());
    }

    private void announceLoop() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(
                        () -> {
                            destination.announce(appData1);
                            log.info("Sent announce for {}", destination.getHexHash());
                        }, 15,15, TimeUnit.SECONDS);
    }


    //private void initConfig(String configDir) throws IOException {
    //    var configDir1 = new File(defaultConfigPath);
    //    if (!configDir1.exists()) {
    //        configDir1.mkdir();
    //    }
    //    var configPath = Path.of(configDir1.getAbsolutePath());
    //    Path configFile = configPath.resolve(CONFIG_FILE_NAME);
    //
    //    if (Files.notExists(configFile)) {
    //        var defaultConfig = this.getClass().getClassLoader().getResourceAsStream("reticulum_default_config.yml");
    //        Files.copy(defaultConfig, configFile, StandardCopyOption.REPLACE_EXISTING);
    //    }
    //}
}
