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
//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
//import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import java.time.Instant;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class Announce2App {
    private static final String APP_NAME = "announce_example";

    // Initialise two lists of strings to use as app_data
    static final List<String> FRUITS = List.of("Peach", "Quince", "Date", "Tangerine", "Pomelo", "Carambola", "Grape");
    static final List<String> NOBLE_GASES = List.of("Helium", "Neon", "Argon", "Krypton", "Xenon", "Radon", "Oganesson");

    Reticulum reticulum;
    Identity identity;
    Transport transport;
    public Destination destination1, destination2;
    //private static Logger log = LogManager.getLogger(AnnounceApp.class);
    static final  String defaultConfigPath = new String(".reticulum"); // if empty will look in Reticulums default paths
    
    private void setup(int numberOfDestinations) {
        try {
            reticulum = new Reticulum(".reticulum");
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }
        identity = new Identity();

        destination1 = new Destination(
            identity,
            Direction.IN,
            DestinationType.SINGLE,
            APP_NAME,
            "announcesample",
            "fruits"
        );
        log.info("destination1 hash: "+destination1.getHexHash());
        destination1.setProofStrategy(ProofStrategy.PROVE_ALL);

        if (numberOfDestinations == 2) {
            destination2 = new Destination(
               identity,
               Direction.IN,
               DestinationType.SINGLE,
               APP_NAME,
               "announcesample",
               "noble_gases"
            );
            log.info("destination2 hash: "+destination2.getHexHash());
            destination2.setProofStrategy(ProofStrategy.PROVE_ALL);
        }

        // create a custom announce handler instance
        var announceHandler = new ExampleAnnounceHandler();

        // register announce handler
        transport = Transport.getInstance();
        transport.registerAnnounceHandler(announceHandler);
        log.debug("announce handlers: {}", transport.getAnnounceHandlers());
    }

    public void run(int numberOfDestinations) {
        if (numberOfDestinations < 2) {
            announceLoop(destination1, null);
        } else {
            announceLoop(destination1, destination2);
        }
    }

    public void announceLoop(Destination d1, Destination d2) {
        
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            () -> {
                var random = new Random();
                var fruit = FRUITS.get(random.nextInt(FRUITS.size()));
                d1.announce(fruit.getBytes(UTF_8));
                 
                log.debug("Sent announce from {} ({}), data: {}", Hex.encodeHexString(d1.getHash()), d1.getName(), fruit);

                if (d2 != null) {
                    var nobleGas = NOBLE_GASES.get(random.nextInt(NOBLE_GASES.size()));
                    d2.announce(nobleGas.getBytes(UTF_8));
                    
                    log.debug("Sent announce from {} ({}), data: {}", Hex.encodeHexString(d2.getHash()), d2.getName(), nobleGas);
                }
            }, 15,15, TimeUnit.SECONDS
        );
    }

    public static void main(String[] args) throws IOException {
        // main to run application directly
        var instance = new Announce2App();
        if ("1".equals(args[0])) {
            instance.setup(1);
            instance.run(1);
        } else {
            instance.setup(2);
            instance.run(2);
        }
    }

    private class ExampleAnnounceHandler implements AnnounceHandler {
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
    }
}
